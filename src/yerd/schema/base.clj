(ns yerd.schema.base
  (:require
   [malli.core :as m]))

;; --------------------------------------------------
;; HELPERS

(def optional {:optional true})
(def closed   {:closed true})
(def open     {:closed false})

(defn safe
  "Return `(f v) or v if there was an exception.`"
  [v f]
  (try
    (f v)
    (catch Exception _
      v)))

;; --------------------------------------------------
;; SCHEMAS
(def String>0 (m/schema [:string {:min 1}]))
