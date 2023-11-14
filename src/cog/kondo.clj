(ns cog.kondo
  "Wrapper namespace for interfacing with clj-kondo."
  {:clj-kondo/config {:skip-comments false}}
  (:require
   [clj-kondo.core   :as k]
   [malli.core       :as m]
   [malli.util       :as m.u]
   [yerd.schema.core :as s]
   [yerd.schema.base :refer [closed open optional String>0]]))
#_ (remove-ns (ns-name *ns*))

(def Coord
  (m/schema :int {:min 1}))

(defn make-coord
  [prefix k]
  (let [k (if-not prefix
            k
            (keyword (str prefix "-" (name k))))]
    [k Coord]))

(defn make-start-pos
  [name]
  [:map
   (make-coord name :row)
   (make-coord name :col)])

(defn make-end-pos
  [name]
  (let [prefix (if name
                 (str name "-end")
                 "end")]
    [:map
     (make-coord prefix :row)
     (make-coord prefix :col)]))

(defn make-pos
  [name]
  (m.u/merge
   (make-start-pos name)
   (make-end-pos name)))

(def NSDef
  (-> [:map
       [:name                :symbol]
       [:doc        optional String>0]
       [:filename            String>0]
       [:author     optional :string]
       [:deprecated optional :boolean]
       [:lang       optional :keyword]
       [:meta       optional [:map open]]]
      (m.u/merge (make-pos nil))
      (m.u/merge (make-pos "name"))
      m.u/closed-schema))

(def NSUsage
  (-> [:map
       [:from              :symbol]
       [:to                :symbol]
       [:filename          String>0]
       [:alias    optional :symbol]
       [:lang     optional :keyword]]
      (m.u/merge (make-start-pos nil))
      (m.u/merge (make-pos "name"))
      (m.u/merge (m.u/optional-keys (make-pos "alias")))
      m.u/closed-schema))

(def VarUsage
  (-> [:map
       [:name                       :symbol]
       [:from                       :symbol]
       [:to                         [:or
                                     :symbol
                                     [:enum :clj-kondo/unknown-namespace]]] ; unresolved symbol e.g. in comment
       [:filename                   String>0]
       [:alias             optional :symbol]
       [:from-var          optional :symbol]
       [:macro             optional :boolean]
       [:private           optional :boolean]
       [:deprecated        optional [:or :boolean String>0]]
       [:refer             optional :boolean]
       [:defmethod         optional :boolean]
       [:dispatch-val-str  optional String>0]
       [:lang              optional :keyword]
       [:arity             optional :int]
       [:fixed-arities     optional [:set :int]]
       [:varargs-min-arity optional :int]]
      (m.u/merge (m.u/optional-keys (make-pos nil))) ; :row and :col may be nil
      (m.u/merge (m.u/optional-keys (make-pos "name")))
      m.u/closed-schema))

(def Analysis
  (m/schema
   [:map closed
    [:keywords              [:vector :map]]
    [:namespace-definitions [:vector :map #_NSDef]]
    [:namespace-usages      [:vector :map #_NSUsage]]
    [:var-definitions       [:vector :map]]
    [:var-usages            [:vector :map #_VarUsage]]]))

(def Result
  (m/schema
   [:map closed
    [:findings [:vector :map]]
    [:config :map]
    [:summary :map]
    [:analysis Analysis]]))

(def coerce #(s/coerce % Result))

(defn- check-vals
  "Check a vector of values in kondo analysis."
  [analysis k schema]
  (let [{:keys [values errors]}
        (reduce (fn [m value]
                  (let [v (s/check value :decode/remove-nil schema)
                        k (if (s/error? v) :errors :values)]
                    (update m k (fnil conj []) v)))
                {}
                (analysis k))]
    (cond-> analysis
      true (assoc k values)
      (seq errors) (assoc-in [:errors k] errors))))

(defn check
  "Takes a kondo `result` and checks `:analysis` using the [[Result]] schema.
  Returns `result` with coerced `:analysis`.
  Schema errors will be returned under `[:analysis :errors]`."
  [result]
  (let [result (coerce result)
        analysis (-> result
                     :analysis
                     (check-vals :namespace-definitions NSDef)
                     (check-vals :namespace-usages NSUsage)
                     (check-vals :var-usages VarUsage))]

    (assoc result :analysis analysis)))

(defn kondo-analyze
  [paths]
  (-> {:lint paths

       ;; :skip-lint true ; not much difference in speed

       ;; If not disabled, throws with: Not supported: class clojure.core$vec
       :cache false

       :config {:output {:analysis {:keywords true
                                    ;; return all meta data
                                    :namespace-definitions {:meta true}}}}}
      k/run!))

(defn analyze
  [paths]
  (-> paths
      kondo-analyze
      check))



(comment
  (time ; 45s
   (def RESULT
     (kondo-analyze
      ["/Users/jonmillett/dividend/src/dvd/chaos/src/clj"
       "/Users/jonmillett/dividend/src/dvd/chaos/test/clj"
       "/Users/jonmillett/dividend/src/dvd/chaos/cc-base/src/main/clojure"])))
  ;; 54s w/ skip lint WTF

  (time ; 1.5s
   (do
     (check RESULT)
     :done))

  (-> RESULT check :analysis :namespace-definitions count)
  (-> RESULT check :analysis :errors :namespace-definitions count)

  (-> RESULT check :analysis :namespace-usages count)
  (-> RESULT check :analysis :errors :namespace-usages count)

  (-> RESULT check :analysis :var-usages count)
  (-> RESULT check :analysis :errors :var-usages count)

  (-> RESULT check :analysis :errors keys)
  (-> RESULT check :analysis :errors :namespace-definitions first)

  :end)

(comment
  (require '[clojure.pprint :refer [pprint]])
  ;; Try to analyze from stdin
  (defn pp-str
    [& forms]
    (with-out-str
      (doseq [form forms]
        (pprint form)
        (println))))

  ;; TODO seems like a kondo bug, doesn't list namespace definitions or usage
  ;; ns forms have to be quoted or ns gets switched
  (let [forms ['(ns foo.bar)
               '(defn baz [] :baz)
               '(ns foo
                  (:require [foo.bar :as bar]))
               '(bar/baz)]
        s     (pp-str forms)
        r     (clojure.java.io/reader (char-array s))]

    ;; (contains? (get-thread-bindings) #'clojure.core/*in*) ; check for a thread binding

    (binding [*in* r]
      (-> ["-"] kondo-analyze (dissoc :config))
      ))


  :end)
