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
    [darktower.server.model :as model]
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

(defn broadcast-game-state [players event-and-payload]
  (doseq [player players]
    ((:send-fn channel-socket) (:uid player) event-and-payload)))

(defmulti event :id)

(defmethod event :default [{:keys [event]}]
  (log/info "Unhandled event: " event))

(defmethod event :darktower/new-game [{:keys [uid ?data] :as ev-msg}]
  (log/info "ev-msg keys" (keys ev-msg))
  (let [new-game-token (model/game-token)
        {:keys [player-name difficulty]} ?data]
    (log/info
      "new-game:" new-game-token
      "difficulty:" difficulty
      "initialized by:" player-name
      "with uid:" uid)
    (model/initialize-game! uid new-game-token player-name difficulty)
    ((:send-fn channel-socket) uid [:darktower/new-game-initialized (get @model/app-state new-game-token)])
    (log/debug "current app-state:" @model/app-state)))

(defmethod event :darktower/join-game [{:keys [uid ?data]}]
  (let [{:keys [player-name joining-game-token]} ?data
        game-state (get @model/app-state joining-game-token)]
    (when game-state
      (log/info "join-game:" uid player-name joining-game-token)
      (model/join-game! uid player-name joining-game-token)
      (let [players (get-in @model/app-state [joining-game-token :players])]
          (broadcast-game-state players [:darktower/player-joined (get @model/app-state joining-game-token)])))
    (log/debug "current app-state:" @model/app-state)))

(defmethod event :darktower/start-game [{:keys [?data]}]
  (let [game-state (get @model/app-state ?data)]
    (when game-state
      (log/info "start-game:" ?data)
      (model/start-game! ?data)
      (let [players (get-in @model/app-state [?data :players])]
          (broadcast-game-state players [:darktower/game-started (get @model/app-state ?data)])))))

(defmethod event :darktower/territory-click [{:keys [uid ?data]}]
  (let [{:keys [token territory-info]} ?data]
    (model/move! uid token territory-info)
    (let [players (get-in @model/app-state [token :players])]
      (broadcast-game-state players [:darktower/territory-clicked (get @model/app-state token)]))))

(defmethod event :darktower/end-turn [{:keys [?data]}]
  (let [{:keys [token]} ?data]
    (model/end-turn! token)
    (let [players (get-in @model/app-state [token :players])]
      (broadcast-game-state players [:darktower/turn-ended (get @model/app-state token)]))))

(defmethod event :darktower/fight [{:keys [uid ?data]}]
  (let [{:keys [token]} ?data]
    (model/fight! uid token)
    (let [players (get-in @model/app-state [token :players])]
      (broadcast-game-state players [:darktower/fought (get @model/app-state token)]))))

(defmethod event :darktower/flee [{:keys [uid ?data]}]
  (let [{:keys [token]} ?data]
    (model/flee! uid token)
    (let [players (get-in @model/app-state [token :players])]
      (broadcast-game-state players [:darktower/fled (get @model/app-state token)]))))

(defmethod event :darktower/next-item [{:keys [uid ?data]}]
  (let [{:keys [token]} ?data]
    (model/next-item! uid token)
    (let [players (get-in @model/app-state [token :players])]
      (broadcast-game-state players [:darktower/bazaar (get @model/app-state token)]))))

(defmethod event :darktower/haggle [{:keys [uid ?data]}]
  (let [{:keys [token]} ?data]
    (model/haggle! uid token)
    (let [players (get-in @model/app-state [token :players])]
      (broadcast-game-state players [:darktower/bazaar (get @model/app-state token)]))))

(defmethod event :darktower/buy-item [{:keys [uid ?data]}]
  (let [{:keys [token]} ?data]
    (model/buy-item! uid token)
    (let [players (get-in @model/app-state [token :players])]
      (broadcast-game-state players [:darktower/bazaar (get @model/app-state token)]))))

(defmethod event :darktower/curse [{:keys [uid ?data]}]
  (let [{:keys [token cursed-player]} ?data]
    (log/info "?data" ?data)
    (model/curse-player! uid token cursed-player)
    (let [players (get-in @model/app-state [token :players])]
      (broadcast-game-state players [:darktower/player-cursed (get @model/app-state token)]))))

(defmethod event :darktower/skip-curse [{:keys [uid ?data]}]
  (let [{:keys [token]} ?data]
    (let [players (get-in @model/app-state [token :players])]
      (broadcast-game-state players [:darktower/turn-ended (get @model/app-state token)]))))

(defmethod event :darktower/next-key [{:keys [uid ?data]}]
  (let [{:keys [token]} ?data]
    (model/next-key! uid token)
    (let [players (get-in @model/app-state [token :players])]
      (broadcast-game-state players [:darktower/dark-tower (get @model/app-state token)]))))

(defmethod event :darktower/try-key [{:keys [uid ?data]}]
  (let [{:keys [token]} ?data]
    (model/try-key! uid token)
    (let [players (get-in @model/app-state [token :players])]
      (broadcast-game-state players [:darktower/dark-tower (get @model/app-state token)]))))

(defn start-router []
  (log/info "Starting router...")
  (defonce router
    (sente/start-chsk-router! (:ch-recv channel-socket) event)))

(defn init []
  (start-websocket)
  (start-router))
