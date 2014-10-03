(ns hatnik.worker.worker-test
  (:require [clojure.test :refer :all]
            [hatnik.worker.worker :refer :all]
            [hatnik.db.storage :as stg]
            [hatnik.db.memory-storage :refer [create-memory-storage]]
            [hatnik.versions :as ver]))

(defn prepare-db-fixture [f]
  (reset! stg/storage (create-memory-storage))
  (let [user-id (stg/create-user! @stg/storage "me@email.com")
        project-id (stg/create-project! @stg/storage {:name "Default"
                                                      :user-id user-id})]
    (stg/create-action! @stg/storage user-id {:project-id project-id
                                              :type "email"
                                              :library "foo-library"
                                              :last-processed-version "1.0.0"})
    (stg/create-action! @stg/storage user-id {:project-id project-id
                                              :type "email"
                                              :library "foo-library"
                                              :last-processed-version "0.9.0"}))
  (f))

(use-fixtures :each prepare-db-fixture)

(deftest test-check-library-and-perform-actions-correct
  (with-redefs [ver/latest-release (constantly "2.0.0")]
    (check-library-and-perform-actions "foo-library" (stg/get-actions @stg/storage)))
  (doseq [action (stg/get-actions @stg/storage)]
    (is (= (:last-processed-version action) "2.0.0"))))

(deftest test-check-library-and-perform-actions-new-version-is-smaller
  (let [actions (stg/get-actions @stg/storage)
        actions-by-id (into {}
                            (map #(vector (:id %) %) actions))]
    (with-redefs [ver/latest-release (constantly "0.5.0")]
      (check-library-and-perform-actions "foo-library" actions))
    (doseq [action (stg/get-actions @stg/storage)]
      (is (= (:last-processed-version action)
             (:last-processed-version (actions-by-id (:id action))))))))

(deftest test-check-library-and-perform-actions-new-version-is-null
  (let [actions (stg/get-actions @stg/storage)
        actions-by-id (into {}
                            (map #(vector (:id %) %) actions))]
    (with-redefs [ver/latest-release (constantly nil)]
      (check-library-and-perform-actions "foo-library" actions))
    (doseq [action (stg/get-actions @stg/storage)]
      (is (= (:last-processed-version action)
             (:last-processed-version (actions-by-id (:id action))))))))



