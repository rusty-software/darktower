(ns darktower.communication
  (:require
    [taoensso.sente :as sente]
    [darktower.autoinit]
    [darktower.config :as config]
    [darktower.model :as model]))

(defn get-chsk-url
  "Connect to a configured server instead of the page host"
  [protocol chsk-host chsk-path type]
  (let [protocol (case type
                   :ajax protocol
                   :ws (if (= protocol "https:") "wss:" "ws:"))]
    (str protocol "//" config/server chsk-path)))

(defonce channel-socket
  (with-redefs [sente/get-chsk-url get-chsk-url]
    (sente/make-channel-socket! "/chsk" {:type :auto})))
(defonce chsk (:chsk channel-socket))
(defonce ch-chsk (:ch-recv channel-socket))
(defonce chsk-send! (:send-fn channel-socket))
(defonce chsk-state (:state channel-socket))

(defn new-game []
  (chsk-send! [:darktower/new-game (:player-name @model/game-state)]))

(defn join-game []
  (let [{:keys [player-name joining-game-token]} @model/game-state]
    (chsk-send! [:darktower/join-game {:player-name player-name
                                       :joining-game-token joining-game-token}])))

(defn start-game []
  (chsk-send! [:darktower/start-game (get-in @model/game-state [:server-state :token])]))

(defn territory-click [territory-info]
  (chsk-send! [:darktower/territory-click {:token (get-in @model/game-state [:server-state :token])
                                           :territory-info (assoc territory-info :kingdom :zenon)}]))

(defmulti event-msg-handler :id)

(defmethod event-msg-handler :default [{:keys [event]}]
  (println "Unhandled event: %s" event))

(defmethod event-msg-handler :chsk/state [{:keys [?data]}]
  (if (= ?data {:first-open? true})
    (println "Channel socket successfully established!")
    (println "Channel socket state change:" ?data)))

(defmethod event-msg-handler :chsk/recv [{:keys [?data] :as msg}]
  (when-let [event (first ?data)]
    (case event
      :darktower/new-game-initialized
      (do
        (model/update-uid! (:uid @(:state msg)))
        (model/update-server-state! (second ?data)))
      :darktower/player-joined
      (do
        (model/update-uid! (:uid @(:state msg)))
        (model/update-server-state! (second ?data)))
      :darktower/game-started
      (model/update-server-state! (second ?data))
      :darktower/territory-clicked
      (do
        (println "territory from server" (second ?data))
        (model/update-server-state! (second ?data)))))
  (println "recv from server:" ?data))

(defonce router
  (sente/start-client-chsk-router! ch-chsk event-msg-handler))
