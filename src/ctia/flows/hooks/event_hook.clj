(ns ctia.flows.hooks.event-hook
  (:require
   [ctia.flows.hook-protocol :refer [Hook]]
   [ctia.events.producers.es.producer :as esp]))

(defrecord ESEventProducerRecord [conn]
  Hook
  (init [_] (do
              (reset! conn (esp/init-producer-conn))
              (comment println "ESEventProducerRecord Initialized: " (pr-str @conn))))
  (destroy [_] :nothing)
  (handle [_ _ object _]
    (comment println "Event Sent to ES:"
             (with-out-str (clojure.pprint/pprint object)))
    (try (when (some? @conn)
           (esp/handle-produce-event @conn
                                     ;; :model is provided as String (we can't JSON export schema)
                                     (update-in object [:model] pr-str)))
         (catch Exception e
           (clojure.pprint/pprint e)))))

(def ESEventProducerHook
  (ESEventProducerRecord. (atom {})))
