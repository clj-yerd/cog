(ns bar.api
  {:cog.ns/type :interface}
  (:require
   [bar.core :as core]))

(defn mogrify
  []
  ;; api ns can use impl ns
  (core/shazzam!))
