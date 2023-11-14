(ns yerd.fs
  (:require
   [babashka.fs :as fs]))

(defn directory?
  [dir]
  (try
    (fs/directory? dir)
    (catch Exception _
      false)))
#_(directory? nil)
#_(directory? 3)
#_(directory? ".")
