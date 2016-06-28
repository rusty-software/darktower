(ns darktower.server.game)

(defn initialize-player [player]
  (assoc player :current-territory {:kingdom (:kingdom player) :row 5 :idx 3}
                :warriors 10
                :gold 30
                :food 25))

(defn initialize-game [players]
  (let [init-players (map initialize-player players)]
    {:players init-players
     :player-order (vec (map :uid init-players))
     :current-player (:uid (first init-players))}))

(defn move [player {:keys [kingdom row idx]}]
  (assoc player :current-territory {:kingcom kingdom :row row :idx idx}))
