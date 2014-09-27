(ns hatnik.db.storage)

(defprotocol UserStorage
  "Protocol for storing users."
  (get-user [storage email] "Returns user if exists on nil.")
  (create-user! [storage email] "Creates new user and returns id."))

(defprotocol ProjectStorage
  "Protocol for storing projects."
  (get-projects [storage user-id] "Returns list of all projects for given user.")
  (create-project! [storage data] "Creates project. Returns id.")
  (update-project! [storage user-id id data] "Updates project.")
  (delete-project! [storage user-id id] "Deletes project."))

(defprotocol ActionStorage
  "Protocol for storing actions."
  (get-actions [storage user-id project-id] "Returns list of actions for given project.")
  (create-action! [storage user-id data] "Creates action. Returns id.")
  (update-action! [storage user-id id data] "Updates project.")
  (delete-action! [storage user-id id] "Deletes action."))

(def storage (atom nil))
