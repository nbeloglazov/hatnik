(ns hatnik.db.storage)

(defprotocol UserStorage
  "Protocol for operating on users."
  (get-user [storage email] "Returns user if exists on nil.")
  (create-user! [storage email] "Creates new user and returns id."))

(defprotocol ProjectStorage
  "Abstract protocol for storing data like projects, actions."
  (get-projects [storage user-id] "Returns list of all projects for given user.")
  (create-project! [storage data] "Creates project. Returns id.")
  (update-project! [storage user-id id data] "Updates project.")
  (delete-project! [storage user-id id] "Deletes project."))

(def storage (atom nil))
