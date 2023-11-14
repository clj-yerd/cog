(ns yerd.schema.core-test
  (:require
   [clojure.test     :refer [deftest is are testing]]
   [malli.core       :as m]
   [malli.transform  :as m.t]
   [yerd.schema.core :as sut]
   [yerd.util        :as u]))

(deftest normalize-transforms-test
  ;; decode variations
  (are [in] (= [m/decoder [m.t/string-transformer]]
               (sut/normalize-transforms in))
    :decode/string
    [:decode :string]
    [:decode [:string]]
    [:decode [m.t/string-transformer]])

  (are [in out] (= out (sut/normalize-transforms in))
    ;; encode
    :encode/json [m/encoder [m.t/json-transformer]]

    ;; bespoke transform
    :encode/postgres [m/encoder [{:name :postgres}]]

    ;; composition
    [:encode [:strip :json]] [m/encoder [m.t/strip-extra-keys-transformer
                                         m.t/json-transformer]]))

(deftest check-test
  (are [args expected] (= expected (apply sut/check args))
    [33 :int]                  33
    ["33" :decode/string :int] 33)

  (testing "validation failure"
    ;; Note: Not checking :schema and :errors values since:
    ;; - :schema is dynamically reified and causes = to fail
    ;; - :errors contains :schema
    (is (= (-> "33"
               (sut/check :int)
               (dissoc :schema :errors))
           {:type    :yerd.schema/error
            :value   "33"
            :summary ["should be an integer"]}))))

(deftest coerce-test
  (try
    (sut/coerce "33" :int)
    (catch Exception x
      (is (-> x ex-data sut/error?)))))

(deftest coerce2-test
  (is (= {:a 1}
         (sut/coerce {:a 1, :b nil} :decode/remove-nil [:map {:decode/remove-nil u/remove-nil}]))))
