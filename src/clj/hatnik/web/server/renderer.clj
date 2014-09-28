(ns hatnik.web.server.renderer
  (:require [hiccup.core :as hc]))

(defn page-html-head []
  (hc/html
   [:head
    [:title "Hatnik"]
    [:link {:rel "stylesheet" :href "/css/bootstrap.min.css"}]]]))


(defn page-header []
  [:nav.navbar.navbar-default {:role "navigation"}
   [:div#navbarCollapse.collapse.navbar-collapse
    [:ul.nav.navbar-nav.navbar-right
     [:li
      [:a.btn "Login via GitHub"]]]]])

(defn core-page [req]
  (hc/html
   (page-html-head)
   
   [:body

    [:div.container
     (page-header)
     [:div.page-header
      [:h2 "Project list"]]
     
     [:div.row      
      [:div.col-md-2
       [:a.btn.btn-success 
        {:href "#" 
         :onclick "$('#iModalProject').modal()"} "Create new"]]
      [:div.col-md-10]]
     [:div
      [:p ""]]

     [:div#iProjectList.panel-group "Loading..."]]

    [:div#iModal.modal.fade {:data-keyboard "true"}
     [:div#iModalDialog.modal-dialog
      [:div.modal-content
       [:div#iActionFormHeader.modal-header]
       [:div#iActionFormBody.modal-body]
       [:div#iActionFormFooter.modal-footer]]]]

    [:div#iModalProject.modal.fade {:data-keyboard "true"}
     [:div.modal-dialog
      [:div.modal-content
       [:div.modal-header
        [:h4.modal-title "Creating a new project"]]
       [:div.modal-body
        [:form
         [:input#project-name-input.form-control 
          {:type "text"
           :placeholder "Project name"}]]]
      [:div.modal-footer
        [:button.btn.btn-primary {:onClick "hatnik.web.client.z_actions.send_new_project_request()"} "Create"]]]]]

    [:script {:src "/js/jquery-2.1.1.min.js"}]    
    [:script {:src "/js/bootstrap.min.js"}]
    [:script {:src "http://fb.me/react-0.11.1.js"}]
    [:script {:src "/gen/js/hatnik.js"}]]))
