(ns cog.api
  {:cog.ns/type :interface}
  (:require
   [cog.core :as core]))

(defn analyze
  [context]
  (core/analyze context))

(comment
  (time ; 45s
   (def RESULT
     (analyze {:paths
               ["/Users/jonmillett/dividend/src/dvd/chaos/src/clj"
                "/Users/jonmillett/dividend/src/dvd/chaos/test/clj"
                "/Users/jonmillett/dividend/src/dvd/chaos/cc-base/src/main/clojure"]})))
  (-> RESULT :kondo :analysis keys)
  (-> RESULT :lint :errors count)
  (-> RESULT :lint :errors first)
  (-> RESULT :lint :errors)
  (-> RESULT :lint :errors (->> (map :to-component)) distinct sort)
  ;; TODO FIX
  (-> RESULT :lint :errors (->> (filter (comp #{'crawlingchaos.domain.credit.report} :to-component))))
  (-> RESULT :lint :errors (->> (filter (comp #{'crawlingchaos.domain.credit.event} :to-component))))
  (-> RESULT :lint :errors (->> (filter (comp #{'crawlingchaos.domain.credit.inquiry} :to-component))))
  (-> RESULT :lint :errors (->> (filter (comp #{'crawlingchaos.batch2} :to-component))))

  {:cog.ns/type :interface}
  ;; - crawlingchaos.domain.credit.report-mock
  ;; - crawlingchaos.domain.credit.inquiry
  ;; - crawlingchaos.domain.credit.bureau.inquiry
  ;; - crawlingchaos.domain.loan.review.flag
  ;; - crawlingchaos.batch2
  ;; - crawlingchaos.domain.credit.event
  ;; - crawlingchaos.domain.credit.outcome

  (-> RESULT :components)


  :end)
