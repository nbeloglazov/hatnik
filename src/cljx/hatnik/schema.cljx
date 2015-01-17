(ns hatnik.schema
  (:require [schema.core :as s]))

(defn string-of-length
  "Create schema that validates string length."
  [min max]
  (s/both s/Str
          (s/pred #(<= min (count %) max)
                  (symbol (str "length-from-" min "-to-" max "?")))))

(def Id
  "Schema for validaing that string matches id.
  We restrict ids to be up to 32 symbols consisting only from
  alphanumeric values. MongoDB uses such ids."
  (s/both (string-of-length 1 32)
          (s/pred #(re-matches #"^[a-zA-Z0-9]+$" %)
                  'alphanumeric?)))

(def ProjectName
  "Schema for validating project names."
  (string-of-length 1 128))

(def Library
  "Schema for validating libraries."
  (string-of-length 1 128))

(def TemplateBody
  "Schema for validating templates that will be used as message bodies.
  For example in email or github issues."
  (string-of-length 1 2000))

(def TemplateTitle
  "Schema for validating templates that will be used as message titles.
  For example in email or github issues."
  (string-of-length 1 256))

(def GithubRepository
  (s/both (s/pred #(re-matches #"(?i)^[A-Z0-9-_.]+/[A-Z0-9-_.]+$" %)
                  'valid-github-repo?)
          (string-of-length 1 128)))

(def ReplaceOperation
  "Schema for replact operation in pull request action."
  {:file (string-of-length 1 1024)
   :regex (string-of-length 1 128)
   :replacement (string-of-length 1 128)})

(def PredefinedOperations
  "List of harcoded operations that can be used instead of manually
  setting them up."
  (s/enum "project.clj"))

(def EmailAction
  {:project-id Id
   :library Library
   :type (s/eq "email")
   :subject TemplateTitle
   :body TemplateBody})

(def NoopAction
  {:project-id Id
   :library Library
   :type (s/eq "noop")})

(def GithubIssueAction
  {:project-id Id
   :library Library
   :type (s/eq "github-issue")
   :title TemplateTitle
   :body TemplateBody
   :repo GithubRepository})

(def GithubPullRequestAction
  {:project-id Id
   :library Library
   :type (s/eq "github-pull-request")
   :title TemplateTitle
   :body TemplateBody
   :repo GithubRepository
   :operations (s/either [ReplaceOperation]
                         PredefinedOperations)})

(def BuildFileAction
  {:project-id Id
   :library Library
   :type (s/eq "build-file")})

(def Action
  "Schema for action. Essentially it is the union of all actions."
  (s/either EmailAction NoopAction GithubIssueAction GithubPullRequestAction))

(def RegularProject
  "Project that has only name. Actions for this project are created by user."
  {:name ProjectName
   :type (s/eq "regular")})

(def BuildFileProject
  "Project that based on a build file. Actions are created by server and cannot
  be modified manually. All actions created based on one, specified in the project itself."
  {:name ProjectName
   :type (s/eq "build-file")
   :build-file (string-of-length 1 1028)
   :action Action})

(def Project
  "Schema for project."
  (s/either RegularProject BuildFileProject))

(comment

  (s/check Id "32")

  )

