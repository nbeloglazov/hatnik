(ns hatnik.db.mongo-storage
  (:require [hatnik.db.storage :refer :all]
            [taoensso.timbre :as timbre]
            [monger.core :as mg]
            [monger.collection :as mc]
            [monger.conversion :refer [to-object-id]]
            [clojure.set :refer [rename-keys]]
            [com.stuartsierra.component :as component]))

(def users "users")
(def projects "projects")
(def actions "actions")

(defn norm-id [map]
  (if map
    (-> map
        (rename-keys {:_id :id})
        (update-in [:id] str))
    map))

(defn has-project? [db user-id project-id]
  (not (nil? (mc/find-one db projects
                          {:_id (to-object-id project-id)
                           :user-id user-id}))))

(defrecord MongoStorage [config connection db]


  hatnik.db.storage.UserStorage
  (get-user [storage email]
    (norm-id (mc/find-one-as-map db users {:email email})))

  (get-user-by-id [storage id]
    (norm-id (mc/find-map-by-id db users (to-object-id id))))

  (create-user! [storage data]
    (if-let [user (get-user storage (:email data))]
      (:id user)
      (-> (mc/insert-and-return db users data)
          norm-id
          :id)))

  (update-user! [storage email data]
    (mc/update db users {:email email}
               (-> data
                   (dissoc data :_id :id)
                   (assoc :email email))
               {:multi false}))


  hatnik.db.storage.ProjectStorage
  (get-projects [storage user-id]
    (->> (mc/find-maps db projects {:user-id user-id})
         (map norm-id)))

  (get-project [storage id]
    (norm-id (mc/find-map-by-id db projects (to-object-id id))))

  (create-project! [storage data]
    (-> (mc/insert-and-return db projects data)
        norm-id
        :id))

  (update-project! [storage user-id id data]
    (mc/update db projects {:_id (to-object-id id)
                            :user-id user-id}
               data {:multi false}))

  (delete-project! [storage user-id id]
    (mc/remove db projects {:_id (to-object-id id)
                            :user-id user-id}))


  hatnik.db.storage.ActionStorage
  (get-actions [storage user-id project-id]
    (if (has-project? db user-id project-id)
      (->> (mc/find-maps db actions {:project-id project-id})
           (map norm-id))))

  (get-actions [storage]
    (->> (mc/find-maps db actions)
         (map norm-id)))

  (create-action! [storage user-id data]
    (if (has-project? db user-id (:project-id data))
      (-> (mc/insert-and-return db actions data)
          norm-id
          :id)
      nil))

  (update-action! [storage user-id id data]
    (let [id (to-object-id id)]
     (when-let [action (mc/find-map-by-id db actions id)]
       (when (has-project? db user-id (:project-id action))
         (mc/update-by-id db actions id data)))))

  (delete-action! [storage user-id id]
    (let [id (to-object-id id)]
     (when-let [action (mc/find-map-by-id db actions id)]
       (when (has-project? db user-id (:project-id action))
         (mc/remove-by-id db actions id)))))


  component/Lifecycle
  (start [storage]
    (timbre/info "Starting MongoStorage component.")
    (let [{:keys [host port db drop?]} config
          conn (mg/connect {:host host
                            :port port})]
      (when drop? (mg/drop-db conn db))
      (assoc storage
        :connection conn
        :db (mg/get-db conn db))))

  (stop [storage]
    (timbre/info "Stopping MongoStorage component.")
    (mg/disconnect connection)
    (assoc storage
      :connection nil
      :db nil)))

(comment

  (def st (-> (map->MongoStorage {:config   {:host "localhost"
                                             :port 27017
                                             :db "test-me"}})
              (component/start)))

  (get-user st "hello")

  (create-user! st "hello" "token")

  (get-projects st "hello")

  (create-project! st {:user-id "hello"
                       :name "hmm"})

  (update-project! st "hello" "5426b77944ae6f681ab5a422"
                   {:user-id "hello" :name "Hey"})

  )
