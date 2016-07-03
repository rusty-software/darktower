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

(defn safe-move [player destination]
  (let [current-territory (:current-territory player)
        neighbors (board/neighbors-for current-territory)]
    (cond
      (some #{destination} neighbors)
      {:move-result :moved :current-territory (normalized-territory destination)}

      (and
        (= (:kingdom destination) (:kingdom current-territory))
        (:pegasus player))
      {:move-result :moved-pegasus :current-territory (normalized-territory destination)}

      :else
      {:move-result :invalid :reason "Destination must be adjacent to your current territory!" :current-territory current-territory})))

(defn lost [{:keys [scout current-territory]} destination]
  (if scout
    {:move-result :lost-scout :extra-turn true :current-territory destination}
    {:move-result :lost :current-territory current-territory}))

(defn plague [{:keys [healer warriors]} destination]
  (if healer
    {:move-result :plague-healer :warriors (min 99 (+ warriors 2)) :current-territory destination}
    {:move-result :plague :warriors (max 1 (- warriors 2)) :current-territory destination}))

(defn dragon-attack [{:keys [sword warriors gold]} {dragon-warriors :warriors dragon-gold :gold} destination]
  (if sword
    {:move-result :dragon-sword
     :dragon-hoard {:warriors 0 :gold 0}
     :warriors (min 99 (+ warriors dragon-warriors))
     :gold (min 99 (+ gold dragon-gold))
     :current-territory destination}
    (let [warriors-taken (if (= 1 warriors) 0 (max 1 (int (* 0.25 warriors))))
          gold-taken (if (= 1 gold) 0 (max 1 (int (* 0.25 gold))))]
      {:move-result :dragon
       :dragon-hoard {:warriors (+ dragon-warriors warriors-taken) :gold (+ dragon-gold gold-taken)}
       :warriors (- warriors warriors-taken)
       :gold (- gold gold-taken)
       :current-territory destination})))

(defn move [player destination]
  (let [result (safe-move player destination)
        updated-player (if (= :moved-pegasus (:move-result result))
                         (merge (dissoc player :pegasus) result)
                         (merge player result))]
    (log/info "updated-player" updated-player)
    updated-player))
