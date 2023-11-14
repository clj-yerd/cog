(ns cog.schema
  (:require
   [babashka.fs      :as bb.fs]
   [malli.core       :as m]
   [yerd.fs          :as fs]
   [yerd.schema.base :as base :refer [closed optional String>0]]))
#_(remove-ns (ns-name *ns*))

(def Config
  (m/schema
   [:map closed
    ;; Suffix used to identify component interface namespaces.
    [:interface-ns-suffix [:maybe :symbol]]

    ;; Suffix used to identify component test namespaces.
    ;; Test namespaces can access the component implementation namespaces for tests.
    [:test-ns-suffix String>0]]))

(def Directory
  (m/schema
   [:and {:decode/fs #(base/safe % (comp str bb.fs/canonicalize))}
    String>0
    [:fn {:error/fn (fn [{:keys [value]} _]
                      (str value " is not a directory"))}
     fs/directory?]]))

(comment
  (bb.fs/file? ".")
  (and (bb.fs/exists? "deps.edn")
       (not (fs/directory? "deps.edn")))
  :end)

(def StdIn
  (m/schema
   [:enum "-"]))

(def Paths
  (m/schema
   [:set {:decode/fs #(base/safe % set)}
    [:or
     Directory
     StdIn]]))

(def Context
  "Analysis context"
  (m/schema
   [:map closed
    [:paths           Paths]
    [:config          Config]
    [:kondo  optional [:map]]]))
