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

(defn valid-move [player destination]
  (let [current-territory (:current-territory player)
        neighbors (board/neighbors-for current-territory)]
    (cond
      (some #{destination} neighbors)
      {:valid? true}

      (and
        (= (:kingdom destination) (:kingdom current-territory))
        (:pegasus player))
      {:valid? true :pegasus-required? true}

      :else
      {:valid? false :reason "Destination must be adjacent to your current territory!"})))

(defn safe-move [destination pegasus-required?]
  (if pegasus-required?
    {:move-result :moved :pegasus-required? true :current-territory (normalized-territory destination)}
    {:move-result :moved :current-territory (normalized-territory destination)}))

(defn battle [player destination]
  (log/info "battle not implemented"))

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

(defn move-action [roll player destination other-move-info]
  (cond
    (<= roll 50) (safe-move destination (:pegasus-required? other-move-info))
    (<= roll 70) (battle player destination)
    (<= roll 80) (lost player destination)
    (<= roll 90) (plague player destination)
    (<= roll 100) dragon-attack))

(defn roll-result [roll]
  (cond
    (>= 50 roll) :safe-move
    (>= 70 roll) :battle
    (>= 80 roll) :lost
    (>= 90 roll) :plague
    (>= 100 roll) :dragon-attack))

(defn encounter-territory [player game-state])
(defn encounter-location [player location])

(defn move [player game-state]
  (let [territory-type (board/type-for (dissoc (:current-territory player) :kingdom))]
    (if (= :territory territory-type)
      (encounter-territory player game-state)
      (encounter-location player territory-type)))

  #_(let [roll-action (roll-result (rand-nth (range 1 101)))]
    (if (and (= :lost roll-action))
      (assoc player :current-territory (:last-territory player)
                    :encounter-result :lost)))
  #_(let [roll (rand-nth (range 1 101))
            result (-action roll player game-state)
            _ (log/info "move-action result" result)
            updated-player (cond-> player
                             (:warriors result) (assoc :warriors (:warriors result))
                             (:gold result) (assoc :gold (:gold result))
                             (:reason result) (assoc :reason (:reason result))
                             :always (assoc :move-result (:move-result result) :current-territory (:current-territory result)))]
        (log/info "updated-player" updated-player)
        updated-player))
#_(defn move [player destination]
  (let [validation (valid-move player destination)]
    (if (:valid? validation)
      #_(let [roll (rand-nth (range 1 101))
            result (move-action roll player destination {:dragon-hoard dragon-hoard :pegasus-required? (:pegasus-required? validation)})
            _ (log/info "move-action result" result)
            updated-player (cond-> player
                             (:warriors result) (assoc :warriors (:warriors result))
                             (:gold result) (assoc :gold (:gold result))
                             (:reason result) (assoc :reason (:reason result))
                             :always (assoc :move-result (:move-result result) :current-territory (:current-territory result)))]
        (log/info "updated-player" updated-player)
        updated-player)
      (merge player validation))))
