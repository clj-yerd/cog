(ns yerd.schema.core
  "High level validation api over malli.

  ## Motivation
  - smaller focus
  - auto memoize transformers, validators, and explainers
  - easier composition of transformers using keywords
  - value first semantics"
  {:clj-kondo/config '{:skip-comments false}}
  (:require
   [malli.core      :as m]
   [malli.error     :as m.e]
   [malli.transform :as m.t]
   [yerd.anomaly    :as anom]
   [yerd.util       :as u]))

(defn new-error
  [x]
  (assoc x :type :yerd.schema/error))

(defn error?
  [x]
  (and (map? x)
       (= :yerd.schema/error (:type x))))

(defn summarize
  "Call [[malli.error/humanize]] on `result` and assoc that under the key `:summary`."
  [result]
  (some-> result
          (assoc :summary (m.e/humanize result))))

(def remove-nil-transformer
  (m.t/transformer
   {:name "remove nil values"
    :decoders {:map u/remove-nil}
    :encoders {:map u/remove-nil}}))

(def transformers
  "Map to lookup built in transformers by keyword.
  Ref: https://github.com/metosin/malli#value-transformation"
  {:string     m.t/string-transformer
   :json       m.t/json-transformer
   :strip      m.t/strip-extra-keys-transformer
   :default    m.t/default-value-transformer
   :key        m.t/key-transformer
   :coll       m.t/collection-transformer
   :remove-nil remove-nil-transformer})

(defn normalize-transforms
  "Returns `transforms` in normalized form.

  **EXAMPLES**

  ## alternate ways to specify a `:string` decoder

  ```clojure
  (normalize-transforms :decode/string)        ; keyword only
  (normalize-transforms [:decode :string])     ; tuple
  (normalize-transforms [:decode [:string]])   ; tuple w/ vec of transforms
  (normalize-transforms [:decode [m.t/string-transformer]]) ; transformer fn
  ;; These all return the same normalized form:
  ;; => [#function[malli.core/decoder]
  ;;     [#function[malli.transform/string-transformer]]]

  ```

  ## json encode transform

  ```clojure
  (normalize-transforms :encode/json)
  ;; => [#function[malli.core/encoder]
  ;;     [#function[malli.transform/json-transformer]]]

  ```

  ## custom encode transform

  ```clojure
  (normalize-transforms :encode/postgres)
  ;; => [#function[malli.core/encoder] [{:name :postgres}]]

  ```

  ## composition of transforms

  ```clojure
  (normalize-transforms [:encode [:strip :json :postgres]])
  ;; => [#function[malli.core/encoder]
  ;;     [#function[malli.transform/strip-extra-keys-transformer]
  ;;      #function[malli.transform/json-transformer]
  ;;      {:name :postgres}]]
  ```
  "
  [transforms]
  (let [[coder transforms]
        (if (keyword? transforms)
          [(-> transforms namespace keyword) [(-> transforms name keyword)]]
          transforms)

        coder
        (case coder
          :encode m/encoder
          :decode m/decoder)

        transforms
        (cond-> transforms
            (not (coll? transforms)) vector)

        transforms
        (->> transforms
             (mapv (fn [x]
                     (cond
                       (keyword? x) (get transformers x {:name x})
                       :else x))))]
    [coder transforms]))

(def transformer
  "Combines `transforms` using [[malli.transform/transformer]] and
  returns a memoized malli transformer for `schema`.
  See [[normalize-transforms]] for example transforms."
  (memoize
   (fn [transforms schema]
     (let [[coder transforms] (normalize-transforms transforms)]
       (->> transforms
            (apply m.t/transformer)
            (coder schema))))))

(def validator
  "Returns a memoized malli validator for `schema`."
  (memoize
   (fn [schema] (m/validator schema))))

(defn transform
  "Transforms `value` using the specified `transforms` and `schema`.
  Returns the transformed value.
  See [[normalize-transforms]] for example transforms."
  [value transforms schema]
  ((transformer transforms schema) value))

(defn valid?
  "Returns true if `value` validates successfully using `schema`."
  [value schema]
  ((validator schema) value))

(def explainer
  "Returns a memoized malli explainer for `schema`."
  (memoize
   (fn [schema] (m/explainer schema))))

(defn explain
  "Returns a validation error explaining why `value` failed to validate using `schema`.
  Returns nil if `value` is valid."
  [value schema]
  (some-> ((explainer schema) value)
          new-error))

(defn check
  "Coerce `value` using the supplied transforms and validate using `schema`.
  Returns the coerced value or a validation error on failure.

  Call [[error?]] to check if a validation error was returned.
  See [[normalize-transforms]] for example transforms."
  ([value schema]
   (check value nil schema))
  ([value transforms schema]
   (let [value (cond-> value
                 transforms (transform transforms schema))]
     (if (valid? value schema)
       value
       (-> value
           (explain schema)
           summarize)))))

 (defn coerce
  "Calls [[check]] on the passed `value`, `transforms`, and `schema`.
  Returns the coerced value.
  Throws on validation failure."
  ([value schema]
   (coerce value nil schema))
  ([value transforms schema]
   (let [result (check value transforms schema)]
     (when (error? result)
       (throw (anom/ex-invalid "Invalid value" result)))
     result)))
