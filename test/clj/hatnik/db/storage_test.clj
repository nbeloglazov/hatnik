(ns hatnik.db.storage-test
  (:require [clojure.test :refer :all]
            [hatnik.db.storage :as s]))

(def foo-email "foo@email.com")
(def bar-email "bar@email.com")

(def foo-user
  {:email foo-email
   :github-token "foo-token"
   :github-username "foo"})

(def bar-user
  {:email bar-email
   :github-token "bar-token"
   :github-username "bar"})

(defn test-user-storage [storage]
  (is (every? nil? (map #(s/get-user storage %)
                        [foo-email bar-email]))
      "Storage should be empty")

  (let [foo-id (s/create-user! storage foo-user)
        foo-id-2 (s/create-user! storage foo-user)
        bar-id (s/create-user! storage bar-user)]

    (is (= (s/get-user storage foo-email)
           (assoc foo-user :id foo-id))
        "Foo should match.")
    (is (= (s/get-user storage bar-email)
           (assoc bar-user :id bar-id))
        "Bar should match.")

    (is (= (s/get-user-by-id storage foo-id)
           (assoc foo-user :id foo-id)))

    (is (= (s/get-user-by-id storage bar-id)
           (assoc bar-user :id bar-id)))

    (is (not= foo-id bar-id) "id should be different")
    (is (= foo-id foo-id-2) "Multiple creation should return same user.")))

(def user1 "user1")
(def user2 "user2")

(defn test-project-storage [storage]
  (is (and (empty? (s/get-projects storage user1))
           (empty? (s/get-projects storage user2)))
      "Initial storage should be empty")

  (let [id1 (s/create-project! storage {:name "Foo project"
                                        :user-id user1})
        id2 (s/create-project! storage {:name "Foo project 2"
                                        :user-id user1})
        id3 (s/create-project! storage {:name "Bar project"
                                        :user-id user2})]

    (is (= (set (s/get-projects storage user1))
           #{{:name "Foo project"
              :user-id user1
              :id id1}
             {:name "Foo project 2"
              :user-id user1
              :id id2}})
        "Projects for user 1 should match")

    (is (= (set (s/get-projects storage user2))
           #{{:name "Bar project"
              :user-id user2
              :id id3}})
        "Projects for user 2 should match")

    ; Get by id
    (is (= (s/get-project storage id1)
           {:name "Foo project"
            :user-id user1
            :id id1}))
    (is (= (s/get-project storage id2)
           {:name "Foo project 2"
            :user-id user1
            :id id2}))

    (is (= 3 (count (distinct [id1 id2 id3])))
        "All ids are different")

                                        ; Updating project
    (s/update-project! storage user1 id1 {:name "Just project"
                                          :user-id user1})
    (is (= (set (s/get-projects storage user1))
           #{{:name "Just project"
              :user-id user1
              :id id1}
             {:name "Foo project 2"
              :user-id user1
              :id id2}})
        "'Foo project' should have changed to 'Bar project'")

    ; Trying to update someone elses project
    (s/update-project! storage user1 id3 {:name "I hacked you!"
                                          :user-id user2})

    (is (= (set (s/get-projects storage user2))
           #{{:name "Bar project"
              :user-id user2
              :id id3}})
        "Bar project should not change")

    ; Deleting project
    (s/delete-project! storage user1 id1)
    (is (= (set (s/get-projects storage user1))
           #{{:name "Foo project 2"
              :user-id user1
              :id id2}})
        "'Just project' should have been deleted")

    ; Trying to delete someon elses project
    (s/delete-project! storage user1 id3)
    (is (= (set (s/get-projects storage user2))
           #{{:name "Bar project"
              :user-id user2
              :id id3}})
        "Bar project should not change")))

(defn test-action-storage [storage]
  (let [user1 (s/create-user! storage foo-user)
        user2 (s/create-user! storage bar-user)
        proj1 (s/create-project! storage {:name "Project 1" :user-id user1})
        proj2 (s/create-project! storage {:name "Project 2" :user-id user2})]

    (is (empty? (s/get-actions storage))
        "Initially storage should be empty")
    (is (empty? (s/get-actions storage user1 proj1))
        "Initially no actions for project 1")
    (is (empty? (s/get-actions storage user2 proj2))
        "Initially no actions for project 2")

    (let [act1 (s/create-action! storage user1 {:some-data "123"
                                                :project-id proj1})
          act2 (s/create-action! storage user2 {:some-data "111"
                                                :project-id proj2})

          ; Try to create project for another user
          act3 (s/create-action! storage user1 {:some-data "I hacked you"
                                                :project-id proj2})]
      (is (= (set (s/get-actions storage user1 proj1))
             #{{:some-data "123"
                :project-id proj1
                :id act1}})
          "Project 1 should have single action.")
      (is (= (set (s/get-actions storage user2 proj2))
             #{{:some-data "111"
                :project-id proj2
                :id act2}})
          "Project 2 should have single action")
      (is (empty? (s/get-actions storage user1 proj2))
          "User1 should not have access to project2.")
      (is (= (set (s/get-actions storage))
             #{{:some-data "123"
                :project-id proj1
                :id act1}
               {:some-data "111"
                :project-id proj2
                :id act2}}))

      ; Updates
      (s/update-action! storage user1 act1 {:project-id proj1
                                            :some-data "new data"})
      (is (= (set (s/get-actions storage user1 proj1))
             #{{:some-data "new data"
                :project-id proj1
                :id act1}})
          "Action for project1 should be updated.")

      ; Illegal update
      (s/update-action! storage user1 act2 {:project-id proj2
                                            :some-data "I hacked your action"})
      (is (= (set (s/get-actions storage user2 proj2))
             #{{:some-data "111"
                :project-id proj2
                :id act2}})
          "Actions for project2 should not be changed")

      ; Delete
      (s/delete-action! storage user1 act1)
      (is (empty? (s/get-actions storage user1 proj1))
          "The only action in project1 should be deleted")

      ; Illegal delete
      (s/delete-action! storage user1 act2)
      (is (= (set (s/get-actions storage user2 proj2))
             #{{:some-data "111"
                :project-id proj2
                :id act2}})
          "Actions for project2 should not be changed")

      (is (= (set (s/get-actions storage))
             #{{:some-data "111"
                :project-id proj2
                :id act2}})
          "In the end only action2 should be present"))))
