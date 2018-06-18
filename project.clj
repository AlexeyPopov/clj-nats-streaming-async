(defproject alexeypopov/clj-nats-streaming-async "0.0.1-SNAPSHOT"
  :description "an async client for NATS Streaming, wrapping java-nats-streaming client"
  :url "https://github.com/AlexeyPopov/clj-nats-async"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[io.nats/jnats "1.0"]
                 [manifold "0.1.8"]]

  :profiles {:provided {:dependencies [[org.clojure/clojure "1.9.0"]]}})
