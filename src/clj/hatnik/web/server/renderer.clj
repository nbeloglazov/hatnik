(ns hatnik.web.server.renderer
  (:require [hiccup.core :as hc]))

(def ga-script
  [:script
   "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

  ga('create', 'UA-51485241-3', 'auto');
  ga('send', 'pageview');"])

(defn page-html-head []
  (hc/html
   [:head
    [:title "Hatnik"]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1.0"}]
    ga-script
    [:link {:rel "stylesheet" :href "/css/bootstrap.min.css"}]
    [:link {:rel "stylesheet" :href "/css/styles.css"}]]))

(defn github-link [config]
  (str "https://github.com/login/oauth/authorize?"
       "client_id=" (:github-id config)
       "&scope=user:email"))

(defn modal-frames []
  (hc/html
   [:div#iModal.modal.fade {:data-keyboard "true"}]
   [:div#iModalAddAction.modal.fade {:data-keyboard "true"}]
   [:div#iModalProjectMenu.modal.fade ]))

(defn page-menu [config user]
  [:nav.navbar.navbar-default {:role "navigation"}
   [:div.container
    [:div.navbar-header
     [:button.navbar-toggle {:type "button"
                             :data-toggle "collapse"
                             :data-target ".navbar-collapse"}
      [:span.sr-only "Toggle navigation"]
      [:span.icon-bar]
      [:span.icon-bar]
      [:span.icon-bar]]
     [:a.navbar-brand {:href "/"} "Hatnik"]]

    [:div#navbarCollapse.collapse.navbar-collapse
     (if user
                                        ; User logged in
       [:ul.nav.navbar-nav.navbar-right
        [:li
         [:a (:github-login user)]]
        [:li
         [:a.btn {:href "/api/logout"} "Logout"]]]

                                        ; User is not logged in
       [:ul.nav.navbar-nav.navbar-right
        [:li
         [:a.btn
          {:href (github-link config)} "Login via GitHub"]]])]]])

(defn work-main-page []
  (hc/html
   [:div#iAppView ""]))

(defn about-page [config]
  [:div.about
   [:div.image
    [:img {:src "/img/landing-image.svg"}]]

   [:div.signin
    [:span "Never miss a release"]
    [:a.btn-success.btn-lg
     {:role "button"
      :href (github-link config)}
     "Login via GitHub"]]

   [:div.row
    [:div.col-md-4
     [:h3 "Use cases"]
     [:ul
      [:li "Automatically update project dependencies"]
      [:li "Automatically update README when you release library"]
      [:li "Create tracking issues for maintenance tasks when you release library"]]]

    [:div.col-md-4
     [:h3 "Actions"]
     [:ul.actions
      [:li.available [:strong  "Email"]
       " - send an email with custom text."]
      [:li.available [:strong "GitHub Issue"]
       " - create an issue in the selected repo."]
      [:li.available  [:strong "GitHub Pull Request"]
       " - modify files and open pull request."]
      [:li.available [:strong "Noop"]
       " - does nothing. Simply shows latest version."]]]
    [:div.col-md-4
     [:h3 "Supported languages"]
     [:p "Only JVM-based languages are currently supported"]
     [:div.langs
      [:div.lang
       [:img {:src "/img/langs/java.png"
              :alt "java"}]
       "java"]
      [:div.lang
       [:img {:src "/img/langs/clojure.gif"
              :alt "clojure"}]
       "clojure"]
      [:div.lang
       [:img {:src "/img/langs/scala.png"
              :alt "scala"}]
       "scala"]]]
]])

(defn core-page [config user]
  (hc/html
   (page-html-head)

   [:body
    (page-menu config user)

    [:div.container

     (if user
       (work-main-page)
       (about-page config))]

    (when user (modal-frames))

    [:script {:src "/js/jquery-2.1.1.min.js"}]
    [:script {:src "/js/bootstrap.min.js"}]
    [:script {:src "/js/react.min.js"}]
    [:script {:src "/js/jquery.bootstrap-growl.min.js"}]

    (when user
      [:script {:src "/gen/js/hatnik.js"}])]))

