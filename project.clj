(defproject alexeypopov/clj-nats-streaming-async "0.0.9"
  :description "an async client for NATS Streaming, wrapping java-nats-streaming client"
  :url "https://github.com/AlexeyPopov/clj-nats-async"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[io.nats/jnats "2.4.1"]
                 [io.nats/java-nats-streaming "2.1.3" :exclusions [io.nats/jnats]]
                 [manifold "0.1.8"]]

  :profiles {:provided {:dependencies [[org.clojure/clojure "1.10.0"]]}})
