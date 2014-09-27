### Hatnik

Hatnik is a web app which aims to help you to track library releases. The idea is pretty simple: setup actions to be performed once some library is released. Action consists of 2 parts: library to watch and an instruction to perform. Example: send email to my@email.com once when org.clojure/clojurescript releases. Pla to support following actions: email, github issue, github pull request, change wiki on github. Initially we're going to support only email and add others if we have enough time during the contest.

#### Actions

* Email - sends customized email. In progress.
* GitHub Issue - creates issue in selected repot. Not started.
* GitHub Pull Request - creates a pull request with simple modification like "set version of library N to XYZ in project.clj". Not started.
* GitHub Wiki - changes wiki page using string find-and-replace algorithm. Not started.

#### How to run locally

To start server locally just run:

```shell
lein cljsbuild once
lein ring server 8080
```

By default app uses in-memory DB. You can change settings by creating `config.clj` file in root folder. Check [config.clj.example](https://github.com/clojurecup2014/hatnik/blob/master/config.clj.example) for available options.
