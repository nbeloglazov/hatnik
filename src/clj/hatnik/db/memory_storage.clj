(ns hatnik.db.memory-storage
  (:require [hatnik.db.storage :refer :all]))

(defn has-project? [storage user-id project-id]
  (some #(= project-id (:id %)) (get-projects storage user-id)))

(deftype MemoryStorage [atom next-id]


  hatnik.db.storage.UserStorage
  (get-user [storage email]
    (->> (:users @atom)
         (filter #(= email (:email %)))
         first))

  (get-user-by-id [storage id]
    (->> (:users @atom)
         (filter #(= id (:id %)))
         first))

  (create-user! [storage email user-token]
    (if-let [user (get-user storage email)]
      (:id user)
      (let [id (next-id)]
        (swap! atom update-in [:users] conj {:id id
                                             :email email
                                             :user-token user-token})
        id)))


  hatnik.db.storage.ProjectStorage
  (get-projects [storage user-id]
    (->> (:projects @atom)
         (filter #(= user-id (:user-id %)))))

  (get-project [storage id]
    (->> (:projects @atom)
         (filter #(= id (:id %)))
         first))

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
                     projects))))


  hatnik.db.storage.ActionStorage
  (get-actions [storage user-id project-id]
    (if (has-project? storage user-id project-id)
      (->> (:actions @atom)
           (filter #(= (:project-id %) project-id)))
      []))

  (get-actions [storage]
    (:actions @atom))

  (create-action! [storage user-id data]
    (if (has-project? storage user-id (:project-id data))
      (let [id (next-id)]
        (swap! atom update-in [:actions] conj (assoc data
                                                :id id))
        id)
      nil))

  (update-action! [storage user-id id data]
    (swap! atom update-in [:actions]
           (fn [actions]
             (map (fn [action]
                    (if (and (= (:id action) id)
                             (has-project? storage user-id
                                           (:project-id action)))
                      (assoc data
                        :id id)
                      action))
                  actions))))

  (delete-action! [storage user-id id]
    (swap! atom update-in [:actions]
           (fn [actions]
             (remove #(and (= (:id %) id)
                           (has-project? storage user-id
                                         (:project-id %)))
                     actions)))))


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
