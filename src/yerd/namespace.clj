(ns yerd.namespace
  (:require
   [clojure.string :as str]))

(defn split
  [ns-sym]
  (-> ns-sym
      str
      (str/split #"\.")
      (->> (map symbol))))

(defn join
  [parts]
  (->> parts
       (map str)
       (str/join ".")
       symbol))

(defn parent
  [ns-sym]
  (some-> ns-sym
          split
          butlast
          seq
          join))
#_ (parent 'foo)
;; => nil

(defn ends-with?
  [ns-sym suffix]
  (= suffix (last (split ns-sym))))
