(ns hatnik.worker.worker-test
  (:require [clojure.test :refer :all]
            [hatnik.worker.worker :refer :all]
            [hatnik.db.storage :as stg]
            [hatnik.db.memory-storage :refer [map->MemoryStorage]]
            [hatnik.versions :as ver]
            [com.stuartsierra.component :as component]))

(defn get-db []
  (let [db (component/start (map->MemoryStorage {}))
        user-id (stg/create-user! db {:email "me@email.com"
                                      :github-token "token"
                                      :github-login "me"})
        project-id (stg/create-project! db {:name "Default"
                                            :user-id user-id})]
    (stg/create-action! db user-id {:project-id project-id
                                    :type "email"
                                    :library {:name "foo-library"
                                              :type "jvm"}
                                    :last-processed-version "1.0.0"})
    (stg/create-action! db user-id {:project-id project-id
                                    :type "email"
                                    :library {:name "foo-library"
                                              :type "jvm"}
                                    :last-processed-version "0.9.0"})
    db))

(deftest test-check-library-and-perform-actions-correct
  (let [db (get-db)]
   (with-redefs [ver/latest-release-jvm (constantly "2.0.0")]
     (check-library-and-perform-actions {:name  "foo-library"
                                         :type "jvm"}
                                        (stg/get-actions db)
                                        db perform-action-disabled {}))
   (doseq [action (stg/get-actions db)]
     (assert (= (:last-processed-version action) "2.0.0")))

   (component/stop db)))

(deftest test-check-library-and-perform-actions-new-version-is-smaller
  (let [db (get-db)
        actions (stg/get-actions db)
        actions-by-id (into {}
                            (map #(vector (:id %) %) actions))]
    (with-redefs [ver/latest-release-jvm (constantly "0.5.0")]
      (check-library-and-perform-actions {:name "foo-library"
                                          :type "jvm"}
                                         actions
                                         db perform-action-disabled {}))
    (doseq [action (stg/get-actions db)]
      (assert (= (:last-processed-version action)
             (:last-processed-version (actions-by-id (:id action))))))

    (component/stop db)))

(deftest test-check-library-and-perform-actions-new-version-is-null
  (let [db (get-db)
        actions (stg/get-actions db)
        actions-by-id (into {}
                            (map #(vector (:id %) %) actions))]
    (with-redefs [ver/latest-release-jvm (constantly nil)]
      (check-library-and-perform-actions {:name "foo-library"
                                          :type "jvm"} actions
                                         db perform-action-disabled {}))
    (doseq [action (stg/get-actions db)]
      (assert (= (:last-processed-version action)
             (:last-processed-version (actions-by-id (:id action))))))

    (component/stop db)))



