(ns cog.api-test
  (:require
   [babashka.fs  :as fs]
   [clojure.test :refer [deftest is]]
   [cog.api      :as sut]))
#_ (remove-ns (ns-name *ns*))

(defn trim-result
  [result]
  (select-keys result [:components :namespaces :lint]))

(defn analyze
  [path]
  (-> path
      (->> (fs/path "corpus")
           str
           vector
           (hash-map :paths))
      sut/analyze
      trim-result))

(deftest dogfood-test
  (is (= (-> {:paths ["src"]} sut/analyze trim-result)
         '{:components {cog {:name cog, :interface cog.api}},
           :namespaces
           {cog.api     {:name cog.api, :component cog, :type :interface},
            cog.context {:name cog.context, :component cog, :type :implementation},
            cog.schema  {:name cog.schema, :component cog, :type :implementation},
            cog.core    {:name cog.core, :component cog, :type :implementation},
            cog.kondo   {:name cog.kondo, :component cog, :type :implementation},
            cog.config  {:name cog.config, :component cog, :type :implementation}}})))

(deftest happy-test
  (is (= (analyze "happy-no-components")
         {}))

  (is (= (analyze "happy")
         '{:components {bar {:name bar, :interface bar.api}},
           :namespaces
           {bar.api  {:name bar.api, :component bar, :type :interface},
            bar.core {:name bar.core, :component bar, :type :implementation},
            bar.baz  {:name bar.baz, :component bar, :type :implementation}}})))

(defn relativize
  [path]
  (->> path
       (fs/relativize (fs/canonicalize "."))
       str))

(deftest sad-test
  (is (= (-> (analyze "sad")
             (update-in [:lint :errors] (fn [m] (mapv #(update % :filename relativize) m))))
         '{:components {bar {:name bar, :interface bar.api}},
           :namespaces
           {bar.api  {:name bar.api, :component bar, :type :interface},
            bar.core {:name bar.core, :component bar, :type :implementation},
            bar.baz  {:name bar.baz, :component bar, :type :implementation}},
           :lint
           {:errors
            [{:filename       "corpus/sad/foo.clj",
              :from           foo,
              :to             bar.core,
              :from-component nil
              :to-component   bar
              :name-col       5,
              :name-row       4,
              :message        "component implementation should not be accessed externally"}
             {:filename       "corpus/sad/foo.clj",
              :from           foo,
              :to             bar.core,
              :from-component nil
              :to-component   bar
              :name-col       2,
              :name-row       7,
              :message
              "component implementation should not be accessed externally"}]}})))
