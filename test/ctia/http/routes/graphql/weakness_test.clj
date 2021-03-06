(ns ctia.http.routes.graphql.weakness-test
  (:require [clj-momo.test-helpers.core :as mth]
            [clojure.test :refer [deftest is join-fixtures testing use-fixtures]]
            [ctia.test-helpers
             [auth :refer [all-capabilities]]
             [core :as helpers]
             [fake-whoami-service :as whoami-helpers]
             [graphql :as gh]
             [store :refer [test-for-each-store-with-app]]]
            [ctim.examples.weaknesses :refer [new-weakness-maximal]]))

(use-fixtures :once (join-fixtures [mth/fixture-schema-validation
                                    whoami-helpers/fixture-server]))

(def ownership-data-fixture
  {:owner "foouser"
   :groups ["foogroup"]})

(defn init-graph-data [app]
  (let [entity-1 (gh/create-object
                  app
                  "weakness"
                  (-> new-weakness-maximal
                      (assoc :title "Weakness 1")
                      (dissoc :id)))
        entity-2 (gh/create-object
                  app
                  "weakness"
                  (-> new-weakness-maximal
                      (assoc :title "Weakness 2")
                      (dissoc :id)))
        entity-3 (gh/create-object
                  app
                  "weakness"
                  (-> new-weakness-maximal
                      (assoc :title "Weakness 3")
                      (dissoc :id)))
        f1 (gh/create-object app "feedback" (gh/feedback-1 (:id entity-1) #inst "2042-01-01T00:00:00.000Z"))
        f2 (gh/create-object app "feedback" (gh/feedback-2 (:id entity-1) #inst "2042-01-01T00:00:00.000Z"))]
    (gh/create-object app
                      "relationship"
                      {:relationship_type "related-to"
                       :timestamp #inst "2042-01-01T00:00:00.000Z"
                       :target_ref (:id entity-2)
                       :source_ref (:id entity-1)})
    (gh/create-object app
                      "relationship"
                      {:relationship_type "related-to"
                       :timestamp #inst "2042-01-01T00:00:00.000Z"
                       :target_ref (:id entity-3)
                       :source_ref (:id entity-1)})
    {:weakness-1 entity-1
     :weakness-2 entity-2
     :weakness-3 entity-3
     :feedback-1 f1
     :feedback-2 f2}))

(deftest weakness-queries-test
  (test-for-each-store-with-app
   (fn [app]
     (helpers/set-capabilities! app "foouser" ["foogroup"] "user" all-capabilities)
     (whoami-helpers/set-whoami-response app
                                         "45c1f5e3f05d0"
                                         "foouser"
                                         "foogroup"
                                         "user")
     (let [datamap (init-graph-data app)
           weakness-1-id (get-in datamap [:weakness-1 :id])
           weakness-2-id (get-in datamap [:weakness-2 :id])
           weakness-3-id (get-in datamap [:weakness-3 :id])
           graphql-queries (str (slurp "test/data/weakness.graphql")
                                (slurp "test/data/weakness_fragments.graphql"))]

       (testing "weakness query"
         (let [{:keys [data errors status]}
               (gh/query app
                         graphql-queries
                         {:id (get-in datamap [:weakness-1 :id])}
                         "WeaknessQueryTest")]
           (is (= 200 status))
           (is (empty? errors) "No errors")

           (testing "the weakness"
             (is (= (:weakness-1 datamap)
                    (dissoc (:weakness data) :relationships))))

           (testing "relationships connection"
             (gh/connection-test app
                                 "WeaknessQueryTest"
                                 graphql-queries
                                 {:id weakness-1-id
                                  :relationship_type "related-to"}
                                 [:weakness :relationships]
                                 (map #(merge % ownership-data-fixture)
                                      [{:relationship_type "related-to"
                                        :target_ref weakness-2-id
                                        :source_ref weakness-1-id
                                        :timestamp #inst "2042-01-01T00:00:00.000Z"
                                        :source_entity (:weakness-1 datamap)
                                        :target_entity (:weakness-2 datamap)}
                                       {:relationship_type "related-to"
                                        :target_ref weakness-3-id
                                        :source_ref weakness-1-id
                                        :timestamp #inst "2042-01-01T00:00:00.000Z"
                                        :source_entity (:weakness-1 datamap)
                                        :target_entity (:weakness-3 datamap)}]))

             (testing "sorting"
               (gh/connection-sort-test
                app
                "WeaknessQueryTest"
                graphql-queries
                {:id weakness-1-id}
                [:weakness :relationships]
                ctia.entity.relationship/relationship-fields)))

           (testing "feedbacks connection"
             (gh/connection-test app
                                 "WeaknessFeedbacksQueryTest"
                                 graphql-queries
                                 {:id weakness-1-id}
                                 [:weakness :feedbacks]
                                 [(dissoc (:feedback-1 datamap) :id :tlp :type :schema_version)
                                  (dissoc (:feedback-2 datamap) :id :tlp :type :schema_version)])

             (testing "sorting"
               (gh/connection-sort-test
                app
                "WeaknessFeedbacksQueryTest"
                graphql-queries
                {:id weakness-1-id}
                [:weakness :feedbacks]
                ctia.entity.feedback.schemas/feedback-fields))))
         (testing "weaknesses query"
           (testing "weaknesss connection"
             (gh/connection-test app
                                 "WeaknessesQueryTest"
                                 graphql-queries
                                 {"query" "*"}
                                 [:weaknesses]
                                 [(:weakness-1 datamap)
                                  (:weakness-2 datamap)
                                  (:weakness-3 datamap)])

             (testing "sorting"
               (gh/connection-sort-test
                app
                "WeaknessesQueryTest"
                graphql-queries
                {:query "*"}
                [:weaknesses]
                ctia.entity.weakness/weakness-fields)))

           (testing "query argument"
             (let [{:keys [data errors status]}
                   (gh/query app
                             graphql-queries
                             {:query (format "title:\"%s\""
                                             (get-in
                                              datamap
                                              [:weakness-1 :title]))}
                             "WeaknessesQueryTest")]
               (is (= 200 status))
               (is (empty? errors) "No errors")
               (is (= 1 (get-in data [:weaknesses :totalCount]))
                   "Only one weakness matches to the query")
               (is (= (:weakness-1 datamap)
                      (first (get-in data [:weaknesses :nodes])))
                   "The weakness matches the search query")))))))))
