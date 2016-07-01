(ns darktower.server.model
  (:require
    [darktower.server.game :as game]
    [darktower.server.board :as board]
    [taoensso.timbre :as log]))

(defonce app-state
  (atom {}))

(defn game-token []
  (Integer/toString (rand-int (Math/pow 36 6)) 36))

(defn initialize-game [app-state uid token name]
  (let [kingdom (rand-nth board/kingdoms)
        remaining-kingdoms (remove #{kingdom} board/kingdoms)]
    (assoc app-state token {:token token
                            :initialized-by uid
                            :remaining-kingdoms remaining-kingdoms
                            :players [{:uid uid
                                       :name name
                                       :kingdom kingdom}]})))

(defn initialize-game! [uid token name]
  (swap! app-state initialize-game uid token name))

(defn join-game [app-state uid name token]
  (let [game-state (get app-state token)
        players (:players game-state)
        kingdoms (:remaining-kingdoms game-state)
        kingdom (rand-nth kingdoms)
        remaining-kingdoms (remove #{kingdom} kingdoms)
        new-players (conj players {:uid uid
                                   :name name
                                   :kingdom kingdom})
        game-state (assoc game-state :players new-players
                                     :remaining-kingdoms remaining-kingdoms)]
    (assoc app-state token game-state)))

(defn join-game! [uid name token]
  (swap! app-state join-game uid name token))

(defn start-game [app-state token]
  (let [game-state (get app-state token)
        initialized-game (game/initialize-game (:players game-state))]
    (assoc app-state token (merge game-state initialized-game {:game-on? true}))))

(defn start-game! [token]
  (swap! app-state start-game token))

(defn player-by-uid [game-state uid]
  (let [players (:players game-state)]
    (first (filter #(= uid (:uid %)) players))))

(defn move [app-state uid token territory-info]
  (let [game-state (get app-state token)]
    (if (= uid (:current-player game-state))
      (let [player (player-by-uid game-state uid)
            moved-player (game/move player territory-info)
            game-state (assoc game-state :players (conj (remove #(= uid (:uid %)) (:players game-state)) moved-player))]
        (assoc app-state token game-state))
      app-state)))

(defn move! [uid token territory-info]
  (swap! app-state move uid token territory-info))
