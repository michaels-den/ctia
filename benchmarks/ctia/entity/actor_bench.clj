(ns ctia.entity.actor-bench
  (:require [ctia.test-helpers
             [benchmark :refer [cleanup-ctia!
                                setup-ctia-es-store!]]
             [core :as helpers :refer [POST]]]
            [ctim.examples.actors
             :refer [new-actor-minimal
                     new-actor-maximal]]
            [perforate.core :refer :all]))

(def small-actor new-actor-minimal)
(def big-actor (dissoc new-actor-maximal :id))

(defgoal create-actor "Create Actor"
  :setup (fn [] [(setup-ctia-es-store!)])
  :cleanup (fn [{:keys [app]}] (cleanup-ctia! app)))

(defn play [app fixture]
  (POST app
        "ctia/actor"
        :body fixture
        :headers {"Authorization" "45c1f5e3f05d0"}))

(defcase create-actor :big-actor-es-store
  [{:keys [app]}] (play app big-actor))

(defcase create-actor :small-actor-es-store
  [{:keys [app]}] (play app small-actor))
