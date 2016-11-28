(ns ctia.http.routes.observable.judgements-indicators-test
  (:refer-clojure :exclude [get])
  (:require [clj-momo.test-helpers.core :as mht]
            [clojure.test :refer [is join-fixtures testing use-fixtures]]
            [ctia.flows.crud :refer [make-id]]
            [ctia.properties :refer [properties]]
            [ctia.test-helpers
             [auth :refer [all-capabilities]]
             [core :as helpers :refer [get post]]
             [fake-whoami-service :as whoami-helpers]
             [store :refer [deftest-for-each-store]]]
            [ctim.domain.id :as id]))

(use-fixtures :once (join-fixtures [mht/fixture-schema-validation
                                    helpers/fixture-properties:clean
                                    helpers/fixture-properties:events-enabled
                                    whoami-helpers/fixture-server]))

(deftest-for-each-store test-observable-judgements-indicators
  (helpers/set-capabilities! "foouser" "user" all-capabilities)
  (whoami-helpers/set-whoami-response "45c1f5e3f05d0" "foouser" "user")

  (let [http-show (get-in @properties [:ctia :http :show])
        judgement-1-id (id/->id :judgement
                                (make-id "judgement")
                                http-show)
        judgement-2-id (id/->id :judgement
                                (make-id "judgement")
                                http-show)
        judgement-3-id (id/->id :judgement
                                (make-id "judgement")
                                http-show)
        indicator-1-id (id/->id :indicator
                                (make-id "indicator")
                                http-show)
        indicator-2-id (id/->id :indicator
                                (make-id "indicator")
                                http-show)
        relationship-1-id (id/->id :relationship
                                   (make-id "relationship")
                                   http-show)
        relationship-2-id (id/->id :relationship
                                   (make-id "relationship")
                                   http-show)
        observable-1 {:type "ip",
                      :value "10.0.0.1"}
        observable-2 {:type "ip"
                      :value "192.168.1.1"}]

    ;; This judgement should be matched
    (testing "test setup: create judgement-1"
      (let [{status :status}
            (post "ctia/judgement"
                  :body {:id (:short-id judgement-1-id)
                         :observable observable-1
                         :source "source"
                         :priority 99
                         :confidence "High"
                         :severity "Medium"
                         :external_ids ["judgement-1"]}
                  :headers {"API_key" "45c1f5e3f05d0"})]
        (is (= 201 status))))

    ;; This judgement should not be matched (no indicator relationship)
    (testing "test setup: create judgement-2"
      (let [{status :status}
            (post "ctia/judgement"
                  :body {:id (:short-id judgement-2-id)
                         :observable observable-1
                         :source "source"
                         :priority 99
                         :confidence "High"
                         :severity "Medium"
                         :external_ids ["judgement-2"]}
                  :headers {"API_key" "45c1f5e3f05d0"})]
        (is (= 201 status))))

    ;; This judgement should not be matched (different observable)
    (testing "test setup: create judgement-3"
      (let [{status :status}
            (post "ctia/judgement"
                  :body {:id (:short-id judgement-3-id)
                         :observable observable-2
                         :source "source"
                         :priority 99
                         :confidence "High"
                         :severity "Medium"
                         :external_ids ["judgement-3"]}
                  :headers {"API_key" "45c1f5e3f05d0"})]
        (is (= 201 status))))

    ;; This indicator should be found based on the observable/relationship
    (testing "test setup: create indicator-1"
      (let [{status :status}
            (post "ctia/indicator"
                  :body {:id (:short-id indicator-1-id)
                         :producer "producer"
                         :external_ids ["indicator-1"]}
                  :headers {"API_key" "45c1f5e3f05d0"})]
        (is (= 201 status))))

    ;; This indicator should not be found
    (testing "test setup: create indicator-2"
      (let [{status :status}
            (post "ctia/indicator"
                  :body {:id (:short-id indicator-2-id)
                         :producer "producer"
                         :external_ids ["indicator-2"]}
                  :headers {"API_key" "45c1f5e3f05d0"})]
        (is (= 201 status))))

    ;; This is the relationship that should be matched
    (testing (str "test setup: create relationship-1 so that judgement-1 is an "
                  "observation of indicator-1")
      (let [{status :status}
            (post "ctia/relationship"
                  :body {:id (:short-id relationship-1-id)
                         :source_ref (id/long-id judgement-1-id)
                         :relationship_type "observable-of"
                         :target_ref (id/long-id indicator-1-id)
                         :external_ids ["relationship-1"]}
                  :headers {"API_key" "45c1f5e3f05d0"})]
        (is (= 201 status))))

    ;; This relationship should not be matched
    (testing (str "test setup: create relationship-2 so that judgement-3 is an "
                  "observation of indicator-2")
      (let [{status :status}
            (post "ctia/relationship"
                  :body {:id (:short-id relationship-2-id)
                         :source_ref (id/long-id judgement-3-id)
                         :relationship_type "observable-of"
                         :target_ref (id/long-id indicator-2-id)
                         :external_ids ["relationship-2"]}
                  :headers {"API_key" "45c1f5e3f05d0"})]
        (is (= 201 status))))

    (testing "GET /:observable_type/:observable_value/judgements/indicators"
      (let [{status :status
             indicator-ids :parsed-body}
            (get (str "ctia/" (:type observable-1) "/" (:value observable-1)
                      "/judgements/indicators")
                 :headers {"API_key" "45c1f5e3f05d0"})]

        (is (= 200 status))
        (is (= #{(id/long-id indicator-1-id)}
               (set indicator-ids)))))))
