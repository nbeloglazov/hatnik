{
 ; Github keys. Used for user authentication
 :github-id "GITHUB_CLIENT_ID"
 :github-secret "GITHUB_CLIENT_SECRET"

 ; Timbre log level. Available :info, :warn, :error
 :log-level :debug

 ; Type of storage to use. Supported :mongo and :db.
 ; This field also defines what storage to use as cookie storage.
 :db :memory

 ; Config for mongo if :db :mongo is used.
 :mongo {:host "localhost"
         :port 27017
         :db "hatnik"
         ; Defines whether recreate db or use existing.
         :drop? false}

 ; Host and port for worker's web server. It listens for
 ; test action requests on that port.
 :worker-server {:host "localhost"
                 :port 5734}

 ; Port for web server which client talks to.
 :web {:port 8080}

 ; Enables /api/force-login method that logins by email
 ; without any authorization. For dev usage only.
 :enable-force-login false

 ; Enables actions. Sometimes for dev purposes it handy to
 ; disable actions, in that case they will be logged but not
 ; performed.
 :enable-actions true

 ; Settings for sending emails.
 :email {:host "smtp.gmail.com"
         :user "GMAIL_ACCOUNT"
         :ssl true
         :pass "ACCOUNT_PASS"
         :from "GMAIL_ACCOUNT"}

 ; Settings that define how often worker job is run.
 ; The job pulls version of all existing libraries and
 ; performs actions if necessary.
 :quartz {:initial-delay-in-seconds 60
          :interval-in-seconds 600}

 }
