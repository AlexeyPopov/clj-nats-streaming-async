(ns clj-nats-streaming-async.core
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [manifold.stream :as s]
            [clj-nats-async.core :as nats])
  (:import [io.nats.streaming StreamingConnectionFactory StreamingConnection MessageHandler Message AckHandler SubscriptionOptions$Builder]))

(defn create-nats-streaming
  "creates a Nats Streaming connection, returning a Nats object
   - cluster-name : nats streaming cluster name
   - client-id : unique client id
   - urls : nats server urls, either a seq or comma separated"
  [cluster-name client-id & urls]
  (let [servers (flatten (map #(str/split % #",") urls))
        str-servers (str/join "," servers)
        ;; nats-connection (nats/create-nats urls)  ;; TODO Возможно стоит реализовать возможность выбора между адресом сервера и уже готовым соединением nats-connection
        scf (StreamingConnectionFactory. cluster-name client-id)]
    ;; (.setNatsConnection nats-connection) ;; TODO Проблема выше
    (.setNatsUrl scf str-servers)
    (.createConnection scf)))

(defprotocol INatsMessage
  (msg-body [_]))

(defrecord NatsMessage [nats-message]
  INatsMessage
  (msg-body [_]
    (edn/read-string
     (String. (.getData nats-message)
              "UTF-8"))))

(defn- create-nats-subscription
  [nats subject {:keys [queue durable-name] :as opts} stream]
  (let [subscription-options-builder (SubscriptionOptions$Builder.)]
    (if (not (nil? durable-name))
      (-> subscription-options-builder
          (.durableName durable-name)))
    (.. subscription-options-builder
        (deliverAllAvailable))
    (.subscribe
     nats
     subject
     queue
     (reify
       MessageHandler
       (onMessage [_ m]
         (s/put! stream (map->NatsMessage {:nats-message m}))))
     (-> subscription-options-builder
         (.build)))))

(defn create-nats-subscription-callback
  [nats subject {:keys [queue durable-name] :as opts} cb]
  (let [subscription-options-builder (SubscriptionOptions$Builder.)]
    (if (not (nil? durable-name))
      (-> subscription-options-builder
          (.durableName durable-name)))
    (.. subscription-options-builder
        (deliverAllAvailable))
    (.subscribe
     nats
     subject
     queue
     (reify
       MessageHandler
       (onMessage [_ m]
         (cb (msg-body (map->NatsMessage {:nats-message m})))))
     (-> subscription-options-builder
         (.build))))
  nil)


(defn- publish
  "publish a message
  - subject-or-fn : either a string specifying a fixed subject or a
                     (fn [item] ...) which extracts a subject from an item"
  ([nats subject-or-fn] (publish nats subject-or-fn "" {}))
  ([nats subject-or-fn body] (publish nats subject-or-fn body {}))
  ([nats subject-or-fn body {:keys [reply] :as opts}]
   (let [is-subject-fn? (or (var? subject-or-fn) (fn? subject-or-fn))
         subject (if is-subject-fn? (subject-or-fn body) subject-or-fn)]
     (if subject
       (.publish
        nats
        subject
        (.getBytes (pr-str body) "UTF-8")
        (reify ;; TODO Возможно стоит передвавать cb контролирующий выполнение отправки. Возможно сделать есть еще один исходящий стрим и в него сразу класть guid сообщения и на стороне клиента контролировать отправку. При необходимости клиент отправит сообщение заново. Или же реалтзовать это на уроане данной библиотеки, просто попытавись отправить его определенное кол-во раз.
          AckHandler
          (onAck [_ guid err]
            (if (not (nil? err))
              (log/error (str "Error publishing msg id " guid ": " (-> err
                                                                      (.getMessage))))))))
       (log/warn (ex-info
                  (str "no subject "
                       (if is-subject-fn? "extracted" "given"))
                  {:body body}))))))

(defn subscriber
  "returns a a Manifold source-only stream of INatsMessages from a NATS subject.
   close the stream to dispose of the subscription"
  ([nats subject] (subscriber nats subject {}))
  ([nats subject opts]
   (let [stream (s/stream)
         source (s/source-only stream)
         nats-subscription (create-nats-subscription nats subject opts stream)]
     (s/on-closed stream
                  (fn []
                    (log/info "closing NATS subscription: " subject " opts " opts)
                    (.unsubscribe nats-subscription))) ;; TODO В данном месте есть проблема, последние версии NATS Server не возвращают ответ об успехе операции и тут возникает NullPointerException   shaded.nats.com.google.protobuf.AbstractParser.parseFrom (AbstractParser.java:185) https://github.com/nats-io/java-nats-streaming/blob/64fe3990699635100548154b3affa16c79d38a06/src/main/java/io/nats/streaming/SubscriptionImpl.java#L178
     source)))

(defn publisher
  "returns a Manifold sink-only stream which publishes items put on the stream
   to NATS"
  ([nats subject-or-fn]
   (let [stream (s/stream)]
     (s/consume (fn [body]
                  (publish nats subject-or-fn body))
                stream)
     (s/sink-only stream))))

(defn pubsub
  "returns a Manifold source+sink stream for a single NATS subject.
   the source returns INatsMessages, while the sink accepts
   strings"
  ([nats subject] (pubsub nats subject {}))
  ([nats subject opts]
   (let [pub-stream (s/stream)
         sub-stream (s/stream)

         nats-subscription (create-nats-subscription nats subject opts sub-stream)]

     (s/consume (fn [body] (publish nats subject body)) pub-stream)

     (s/on-closed sub-stream (fn [] (.close nats-subscription)))

     (s/splice pub-stream sub-stream))))
