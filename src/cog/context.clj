(ns cog.context
  (:require
   [cog.config       :as config]
   [cog.schema       :refer [Context]]
   [yerd.schema.core :as s]))
#_ (remove-ns (ns-name *ns*))

(def coerce #(s/coerce % :decode/fs Context))

(defn create
  [context]
  (-> context
      (update :config #(merge config/DEFAULTS %))
      coerce))

(comment
  (create {:paths ["src" "src"]})
  :end)
