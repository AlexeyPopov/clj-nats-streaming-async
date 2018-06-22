(defproject alexeypopov/clj-nats-streaming-async "0.0.3"
  :description "an async client for NATS Streaming, wrapping java-nats-streaming client"
  :url "https://github.com/AlexeyPopov/clj-nats-async"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[alexeypopov/clj-nats-async "1.3.0"]
                 [alexeypopov/java-nats-streaming "0.5.0-1"]
                 [manifold "0.1.8"]]

  :profiles {:provided {:dependencies [[org.clojure/clojure "1.9.0"]]}})
