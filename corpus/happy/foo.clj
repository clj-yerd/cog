(ns foo
  (:require
   ;; external ns can use api ns
   [bar.api :as b]))

;; external ns can use api var
(b/mogrify)
