(ns foo
  (:require
   [bar :as b]))

(defn hello
  [x]
  (b/mogrify x))
