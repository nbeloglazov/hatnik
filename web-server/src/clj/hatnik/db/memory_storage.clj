(ns hatnik.db.memory-storage
  (:require [hatnik.db.storage :refer :all]))

(deftype MemoryStorage [atom next-id]


  hatnik.db.storage.UserStorage
  (get-user [storage email]
    (->> (:users @atom)
         (filter #(= email (:email %)))
         first))

  (create-user! [storage email]
    (if-let [user (get-user storage email)]
      (:id user)
      (let [id (next-id)]
        (swap! atom update-in [:users] conj {:id id
                                             :email email})
        id)))


  hatnik.db.storage.ProjectStorage
  (get-projects [storage user-id]
    (->> (:projects @atom)
         (filter #(= user-id (:user-id %)))))

  (create-project! [storage data]
    (let [id (next-id)]
      (swap! atom update-in [:projects] conj (assoc data
                                               :id id))
      id))

  (update-project! [storage user-id id data]
    (swap! atom update-in [:projects]
           (fn [projects]
             (map (fn [project]
                    (if (and (= (:user-id project) user-id)
                             (= (:id project) id))
                      (assoc data
                        :id id)
                      project))
                  projects))))

  (delete-project! [storage user-id id]
    (swap! atom update-in [:projects]
           (fn [projects]
             (remove #(and (= (:user-id %) user-id)
                           (= (:id %) id))
                     projects)))))


(defn create-memory-storage []
  (let [id (atom 0)
        next-id #(str (swap! id inc))]
    (MemoryStorage. (atom {:projects [] :users [] :actions []})
                    next-id)))


(comment

  (def m (create-memory-storage))

  (get-user m "hello")

  (create-user! m "hello")

  )
