(ns yerd.anomaly
  "Cognitect Anomalies but using Malli.

  Ref: https://github.com/cognitect-labs/anomalies

  Differences from Cognitect Anomalies:
  - The anomaly :invalid was added as it is so common. It can be considered a subset of :incorrect.
    - Incorrect = wrong
    - Invalid = wrong through being inappropriate to the situation
    Ref: https://forum.wordreference.com/threads/invalid-incorrect-wrong.2776284/
  - The anomaly :success was added to indicate success. This allows a
    map to include a :status attribute that covers both success
    and any anomalies.
  ")

(def ANOMALIES
  {:busy        {:description "service busy"           :retry true  :fix "backoff and retry"}
   :conflict    {:description "conflicting operation"  :retry false :fix "coordinate callers"}
   :fault       {:description "unexpected error"       :retry true  :fix "fix caller/callee bug"}
   :forbidden   {:description "access denied"          :retry false :fix "fix caller credentials"}
   :incorrect   {:description "incorrect usage"        :retry true  :fix "fix caller bug"}
   :interrupted {:description "processing interrupted" :retry true  :fix "retry"}
   :invalid     {:description "invalid format"         :retry false :fix "fix caller bug"}
   :not-found   {:description "resource not found"     :retry false :fix "fix caller noun"}
   :success     {:description "completed sucessfully"  :retry false :fix "nothing to fix"}
   :unavailable {:description "service unavailable"    :retry true  :fix "check service status; backoff and retry"}
   :unsupported {:description "unsupported operation"  :retry false :fix "fix caller verb"}})

(def Fault       :fault)
(def Success     :success)
(def Unavailable :unavailable)

(def AnomalyKey
  (into [:enum] (keys ANOMALIES)))

(def Anomaly
  [:map
   [:type    AnomalyKey]
   [:message :string]])

(defn anomaly
  ([type- message & {:as data}]
   (merge data
          #:anomoly{:type    type-
                    :message message})))

(defn invalid
  [message & {:as data}]
  (anomaly :invalid message data))

(defn ex-invalid
  [message & {:as data}]
  (ex-info message (invalid message data)))
