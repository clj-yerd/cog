(ns cog.core
  (:require
   [clojure.string :as str]
   [cog.context    :as context]
   [cog.kondo      :as kondo]
   [yerd.namespace :as ns]))
#_ (remove-ns (ns-name *ns*))

(defn kondo-analyze
  [{:keys [paths]
    :as context}]
  (assoc context :kondo (kondo/analyze paths)))

(defn ns-interface-match?
  [{suffix :interface-ns-suffix
    :as _config}
   ns-sym]
  (ns/ends-with? ns-sym suffix))
#_ (ns-interface-match? {} 'foo.api)
;; => false

(defn ns-interface-suffix?
  [config]
  (-> config :interface-ns-suffix boolean))

(defn ignore-ns?
  [ns-def]
  (-> ns-def :meta :cog.lint/ignore boolean))

(defn infer-type
  [config
   {:keys [meta name]
    :as   ns-def}]
  (let [ns-type (:cog.ns/type meta)]
    (cond
      ;; honor ignore if present
      (ignore-ns? ns-def)               nil
      ;; honor explicit type if present
      ns-type                           (#{:interface} ns-type)
      ;; infer from ns suffix
      (ns-interface-match? config name) true)))

(defn infer-name
  [config
   {:keys [meta name]
    :as _ns-def}]
  (or
   ;; honor explicit name if specified
   (:cog.component/name meta)

   ;; match against suffix if configured
   (and (ns-interface-match? config name)
        (ns/parent name))

   ;; if no suffix defined, use the parent
   (and (not (ns-interface-suffix? config))
        (ns/parent name))

   ;; TODO collect errors instead of throwing?
   (throw (ex-info "Component name required" {:name name}))))

(defn index-interface
  [config idx {:keys [name] :as ns-def}]
  (let [type (infer-type config ns-def)]
    (if-not type
      idx
      (let [component-name (infer-name config ns-def)]
        (-> idx
            (assoc-in [:components component-name] {:name      component-name
                                                    :interface name})
            (assoc-in [:namespaces name] {:name      name
                                          :component component-name
                                          :type      :interface}))))))

(defn add-interfaces
  [{:keys [config
           kondo]
    :as context}]
  (let [idx (reduce (partial index-interface config)
                    {}
                    (-> kondo :analysis :namespace-definitions))]
    (merge context
           (select-keys idx [:components :namespaces]))))

(defn find-component
  [components ns-sym]
  (when components
    (loop [sym ns-sym]
      (when sym
        (or (components sym)
            (recur (ns/parent sym)))))))

(defn infer-component
  [components
   {:keys [meta name]
    :as   ns-def}]
  (when-not (ignore-ns? ns-def)
    (or
     ;; honor explicit type if present
     (some-> meta :cog.component/name components)
     ;; infer from ns suffix
     (find-component components name))))

(defn assert-meta-ok
  "Throw if there are :cog meta data fields outside of a component."
  [{:keys [meta]
    :as ns-def}]
  (and
   (not (ignore-ns? ns-def))
   (:cog.ns/type meta)
   (throw (ex-info "Meta data supplied but can't determine component." {:ns-def ns-def}))))

(defn infer-impl-type
  [{:keys [test-ns-suffix] :as
    _config}
   {:keys                 [name]
    {:cog.ns/keys [type]} :meta
    :as                   _ns-def}]
  (cond
    type                                       type
    (str/ends-with? (str name) test-ns-suffix) :test
    :else                                      :implementation))

(defn index-implementation
  [config components idx {:keys [name] :as ns-def}]
  (let [component (infer-component components ns-def)]
    (or component (assert-meta-ok ns-def)) ; TODO collect errors? dynamic var?

    (cond-> idx
      (and component
           (not (contains? idx name)))
      (assoc name {:name      name
                   :component (:name component)
                   :type      (infer-impl-type config ns-def)}))))

(defn add-implementations
  [{:keys [config
           kondo
           components
           namespaces]
    :as context}]
  (let [idx (reduce (partial index-implementation config components)
                    namespaces
                    (-> kondo :analysis :namespace-definitions))]
    (cond-> context
      (seq idx) (assoc :namespaces idx))))

;; LINT

(defn lint-usage
  [{:keys [namespaces]
    :as   _context}
   errors
   {:keys [from to] :as ns-usage}]
  (let [from-component (some-> namespaces from :component)
        to-component   (some-> namespaces to :component)
        error
        (cond
          ;; if to a non component, allow
          (nil? to-component) nil

          ;; if to a component interface, allow
          (some-> namespaces to :type (= :interface)) nil

          ;; if to a component impl/test from the same component, allow
          (= from-component to-component) nil

          ;; else error
          :else (-> ns-usage
                    (select-keys [:from :to :filename :name-col :name-row])
                    (merge {:from-component from-component
                            :to-component   to-component})
                    (assoc :message "component implementation should not be accessed externally")))]
                           (cond-> errors
                             error (conj error))))

(defn lint-usages
  [{:keys [kondo]
    :as   context}]
  (let [usages (concat (-> kondo :analysis :namespace-usages)
                       (-> kondo :analysis :var-usages))
        errors (reduce (partial lint-usage context)
                       []
                       usages)]
    (cond-> context
      (seq errors) (assoc-in [:lint :errors] errors))))

;; INDEX
;; - ? what does it need to do?
;;   - given an unindexed ns, return which component it would be a part of (index of components by name)
;;   - given an indexed ns, return the component name and type (index of namespaces by name)

(defn analyze
  [context]
  (let [{:keys [paths]
         :as context} (context/create context)]
    (-> context
        context/create
        kondo-analyze
        add-interfaces
        add-implementations
        lint-usages)
    ;; TODO add schemas on the context
    ;; TODO lint namespaces
    ;; TODO create and return report
    ))
