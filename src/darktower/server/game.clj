(ns darktower.server.game)

(defn initialize-player [player]
  (assoc player :starting-territory {:kingdom (:kingdom player) :row 5 :idx 3}
                :warriors 10
                :gold 30
                :food 25))

(defn initialize-game [players]
  (let [init-players (map initialize-player players)]
    {:players init-players}))
