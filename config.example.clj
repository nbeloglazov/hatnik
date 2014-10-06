{:github-id "GITHUB_CLIENT_ID"
 :github-secret "GITHUB_CLIENT_SECRET"
 :log-level :debug ; default is :info
 :db :mongo ; or :memory
 :mongo {:host "localhost"
         :port 27017
         :db "hatnik"
         :drop? false ; if true - clears existing db
         }

 :worker-server {:host "localhost"
                 :port 5734}

 :web {:port 8080}

 :enable-force-login false ; if true, enabled /api/force-login method that
                           ; logins by email without any authorization

 :enable-actions true ; if false actions won't be performed, only logged

 :email {:host "smtp.gmail.com"
         :user "GMAIL_ACCOUNT"
         :ssl true
         :pass "ACCOUNT_PASS"
         :from "GMAIL_ACCOUNT"}

 :quartz {:initial-delay-in-seconds 60
          :interval-in-seconds 600}

 }
