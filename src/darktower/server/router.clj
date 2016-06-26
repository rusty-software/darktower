(ns darktower.server.router
  (:require
    [compojure.core :refer [defroutes GET POST]]
    [compojure.route :as route]
    [ring.middleware.cors :as cors]
    [ring.middleware.defaults :as defaults]
    [ring.middleware.reload :as reload]
    [ring.util.response :as response]
    [taoensso.sente :as sente]
    [taoensso.sente.server-adapters.http-kit :as http-kit]
    [taoensso.timbre :as log]
    [darktower.server.board :as board]))

(declare channel-socket)

(defn start-websocket []
  (log/info "Starting websocket...")
  (defonce channel-socket
    (sente/make-channel-socket!
      http-kit/sente-web-server-adapter
      {:user-id-fn (fn [req] (:client-id req))})))

(defroutes routes
  (GET "/" req (response/content-type
                 (response/resource-response "public/index.html")
                 "text/html"))
  (GET "/status" req (str "Running: " (pr-str @(:connected-uids channel-socket))))
  (GET "/chsk" req ((:ajax-get-or-ws-handshake-fn channel-socket) req))
  (POST "/chsk" req ((:ajax-post-fn channel-socket) req))
  (route/resources "/")
  (route/not-found "Not found"))

(def handler
  (-> #'routes
      (reload/wrap-reload)
      (defaults/wrap-defaults (assoc-in defaults/site-defaults [:security :anti-forgery] false))
      (cors/wrap-cors :access-control-allow-origin [#".*"]
        :access-control-allow-methods [:get :put :post :delete]
        :access-control-allow-credentials ["true"])))

(defn broadcast-game-state [uid event-and-payload]
  (log/info "broadcasting" event-and-payload "to" uid)
  ((:send-fn channel-socket) uid event-and-payload)
  #_(doseq [player players]
    ((:send-fn channel-socket) (:uid player) event-and-payload)))

(defmulti event :id)

(defmethod event :default [{:keys [event]}]
  (log/info "Unhandled event: " event))

(defmethod event :darktower/territory-click [{:keys [uid ?data]}]
  (let [{:keys [territory-info]} ?data]
    (log/info "territory-info" territory-info)
    (broadcast-game-state uid [:darktower/territory-clicked (board/neighbors-for territory-info)])
    #_(model/play-card! uid token card from-space)
    #_(let [players (get-in @model/app-state [token :players])]
      (broadcast-game-state players [:cartagena-cs/card-played (get @model/app-state token)]))))

(defn start-router []
  (log/info "Starting router...")
  (defonce router
    (sente/start-chsk-router! (:ch-recv channel-socket) event)))

(defn init []
  (start-websocket)
  (start-router))
