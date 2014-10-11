(ns hatnik.db.memory-storage
  (:require [hatnik.db.storage :refer :all]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]))

(defn has-project? [storage user-id project-id]
  (some #(= project-id (:id %)) (get-projects storage user-id)))

(defrecord MemoryStorage [atom next-id]


  hatnik.db.storage.UserStorage
  (get-user [storage email]
    (->> (:users @atom)
         (filter #(= email (:email %)))
         first))

  (get-user-by-id [storage id]
    (->> (:users @atom)
         (filter #(= id (:id %)))
         first))

  (create-user! [storage data]
    (if-let [user (get-user storage (:email data))]
      (:id user)
      (let [id (next-id)]
        (swap! atom update-in [:users] conj (assoc data
                                              :id id))
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
                     actions))))


  component/Lifecycle
  (start [storage]
    (timbre/info "Starting MemoryStorage component.")
    storage
    (let [id (clojure.core/atom 0)
          next-id #(str (swap! id inc))]
      (assoc storage
        :atom (clojure.core/atom {:projects [] :users [] :actions []})
        :next-id next-id)))

  (stop [storage]
    (timbre/info "Stopping MemoryStorage component.")
    (assoc storage
      :atom nil
      :next-id nil)))
