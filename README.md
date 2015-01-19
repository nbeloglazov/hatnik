### Hatnik

[![Build Status](https://travis-ci.org/nbeloglazov/hatnik.svg)](https://travis-ci.org/nbeloglazov/hatnik/builds)

[Hatnik](http://hatnik.com) is a web app which aims to help you to track library releases. The idea is pretty simple: setup actions to be performed once some library is released. Action consists of 2 parts: the library to watch and instructions to perform. Example: send an email to `my@email.com` once new version of `org.clojure/clojurescript` released. The plan is to support following actions: send an email, create a github issue, create a github pull request, update a wiki on github. So far we support only email and will add others if we have enough time during the contest.

#### Actions

* Email - sends a customized email.
* Noop - does nothing.
* GitHub Issue - creates an issue in a selected repo.
* GitHub Pull Request - creates a pull request with a simple modification like "set version of library N to XYZ in project.clj".
* GitHub Wiki - changes a wiki page using a string find-and-replace algorithm. Not started.

#### How to run locally

To start copy `config.default.clj` to `config.clj` and set `:cljs-optimization-none?` to `true`. And then run:

```shell
lein cljx once
lein cljsbuild once
lein run
```

By default app uses in-memory DB. You can change settings in `config.clj`. Check [config.default.clj](https://github.com/nbeloglazov/hatnik/blob/master/config.default.clj) for available options.

When registering a Github application for your own application, the URI of the "Application callback URL" is /api/github.
