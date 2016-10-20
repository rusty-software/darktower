(ns darktower.server.model
  (:require
    [darktower.server.game :as game]
    [darktower.server.board :as board]
    [taoensso.timbre :as log]))

(defonce app-state
  (atom {}))

(defn game-token []
  (Integer/toString (rand-int (Math/pow 36 4)) 36))

(defn initialize-game [app-state uid token name]
  (let [kingdom (first board/kingdoms)
        remaining-kingdoms (rest board/kingdoms)]
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
        kingdom (first kingdoms)
        remaining-kingdoms (rest kingdoms)
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

(defn next-player
  [current-player player-order]
  (let [current-player-index (.indexOf player-order current-player)]
    (if (= current-player-index (dec (count player-order)))
      (get player-order 0)
      (get player-order (inc current-player-index)))))

(defn rotate-current-player [app-state token]
  (let [game-state (get app-state token)
        next (next-player (:current-player game-state) (:player-order game-state))
        updated-game-state (assoc game-state :current-player next)]
    (assoc app-state token updated-game-state)))

;; TODO: should not be allowed to move while in a fight
(defn move [app-state uid token destination]
  (let [game-state (get app-state token)]
    (if (= uid (:current-player game-state))
      (let [player (player-by-uid game-state uid)
            validation (game/valid-move player destination)]
        (if (:valid? validation)
          (let [updated-player (-> player
                                   (dissoc :message)
                                   (assoc :food (:food (game/feed player))))
                moved-player (cond-> updated-player
                               (:pegasus-required? validation) (dissoc :pegasus)
                               :always (assoc :current-territory (game/normalized-territory destination)
                                              :last-territory (:current-territory player)))
                encounter-result (game/encounter moved-player (:dragon-hoard game-state))
                updated-game-state (cond-> game-state
                                     (:dragon-hoard encounter-result)
                                     (assoc :dragon-hoard (:dragon-hoard encounter-result))

                                     :always
                                     (assoc :players (conj (remove #(= uid (:uid %)) (:players game-state)) (:player encounter-result))))]
            (assoc app-state token updated-game-state))
          (let [updated-player (merge (assoc player :encounter-result :invalid-move) validation)
                updated-game-state (assoc game-state :players (conj (remove #(= uid (:uid %)) (:players game-state)) updated-player))]
            (assoc app-state token updated-game-state))))
      app-state)))

(defn move! [uid token territory-info]
  (swap! app-state move uid token territory-info))

(defn end-turn! [token]
  (swap! app-state rotate-current-player token))

(defn fight [app-state uid token]
  (let [game-state (get app-state token)]
    (if (= uid (:current-player game-state))
      (let [player (dissoc (player-by-uid game-state uid) :message)
            updated-player (game/fight player)
            updated-game-state (assoc game-state :players (conj (remove #(= uid (:uid %)) (:players game-state)) (:player updated-player)))]
        (assoc app-state token updated-game-state))
      app-state)))

(defn fight! [uid token]
  (swap! app-state fight uid token))

(defn flee [app-state uid token]
  (let [game-state (get app-state token)]
    (if (= uid (:current-player game-state))
      (let [player (player-by-uid game-state uid)
            flee-result (game/flee player)
            updated-game-state (assoc game-state :players (conj (remove #(= uid (:uid %)) (:players game-state)) (:player flee-result)))]
        (assoc app-state token updated-game-state))
      app-state)))

(defn flee! [uid token]
  (swap! app-state flee uid token))
