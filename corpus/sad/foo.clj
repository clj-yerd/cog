(ns foo
  (:require
   ;; external ns cannot access impl ns
   [bar.core :as b]))

;; external ns cannot access impl var
(b/shazzam!)
