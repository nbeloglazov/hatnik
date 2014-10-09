### Hatnik

[![Build Status](https://travis-ci.org/nbeloglazov/hatnik.svg)](https://travis-ci.org/nbeloglazov/hatnik/builds)

[Hatnik](http://hatnik.com) is a web app which aims to help you to track library releases. The idea is pretty simple: setup actions to be performed once some library is released. Action consists of 2 parts: the library to watch and an instruction to perform. Example: send an email to `my@email.com` once when org.clojure/clojurescript releases. The plan is to support following actions: send an email, create a github issue, create a github pull request, update a wiki on github. So far we support only email and will add others if we have enough time during the contest.

#### Actions

* Email - sends a customized email. In progress.
* GitHub Issue - creates an issue in a selected repo. Not started.
* GitHub Pull Request - creates a pull request with a simple modification like "set version of library N to XYZ in project.clj". Not started.
* GitHub Wiki - changes a wiki page using a string find-and-replace algorithm. Not started.

#### How to run locally

To start the server locally just run:

```shell
lein cljsbuild once
lein run
```

By default app uses in-memory DB. You can change settings by creating `config.clj` file in root folder. Check [config.example.clj](https://github.com/nbeloglazov/hatnik/blob/master/config.example.clj) for available options.

When registering a Github application for your own application, the URI of the "Application callback URL" is /api/github.
