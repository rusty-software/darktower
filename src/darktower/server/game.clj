(ns darktower.server.game
  (:require [taoensso.timbre :as log]))

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

(defn move [player {:keys [kingdom row idx type]}]
  (let [destination (cond-> {:kingdom kingdom}

                            (not (nil? row))
                            (assoc :row row)

                            (not (nil? idx))
                            (assoc :idx idx)

                            (not (nil? type))
                            (assoc :type type))
        uplayer (assoc player :move-result :moved :current-territory destination)]
    (log/info "uplayer" uplayer)
    uplayer))
