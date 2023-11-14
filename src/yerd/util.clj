(ns yerd.util)

(defn remove-nil
  "Remove nil values."
  [x]
  (cond
    (map? x) (reduce-kv (fn [m k v]
                          (if (nil? v)
                            m
                            (assoc m k v)))
                        {}
                        x)
    :else x))
