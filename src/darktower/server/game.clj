(ns darktower.server.game
  (:require [taoensso.timbre :as log]
            [darktower.server.board :as board]))

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

(defn normalized-territory [{:keys [kingdom row idx type]}]
  (cond-> {:kingdom kingdom}

    (not (nil? row))
    (assoc :row row)

    (not (nil? idx))
    (assoc :idx idx)

    (not (nil? type))
    (assoc :type type)))

(defn move [player destination]
  (let [current-territory (:current-territory player)
        neighbors (board/neighbors-for current-territory)
        result (if (some #{destination} neighbors)
                 {:move-result :moved :current-territory (normalized-territory destination)}
                 {:move-result :invalid :reason "Destination must be adjacent to your current territory!" :current-territory current-territory})

        uplayer (merge player result)]
    (log/info "uplayer" uplayer)
    uplayer))
