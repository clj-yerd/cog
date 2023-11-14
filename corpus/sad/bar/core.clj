(ns bar.core
  (:require
   ;; impl ns can use other impl ns
   [bar.baz :as baz]))

(defn shazzam!
  []
  ;; impl ns can use var in other impl ns
  (baz/bazzam!)
  :shazzam)
