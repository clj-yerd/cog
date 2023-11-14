(ns cog.config
  (:require
   [cog.schema       :refer [Config]]
   [yerd.schema.core :as s]))

(def check #(s/coerce % Config))

(def DEFAULTS
  (check
   {:interface-ns-suffix nil #_'api
    :test-ns-suffix      "-test"}))
