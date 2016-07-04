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

(defn safe-move [player pegasus-required? destination]
  (if pegasus-required?
    {:move-result :moved :pegasus-required? true :current-territory (normalized-territory destination)}
    {:move-result :moved :current-territory (normalized-territory destination)}))

(defn battle [player]
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

(defn roll-action [roll]
  (cond
    (<= roll 50) safe-move #_(with-meta 'safe-move {:name "safe-move"})
    (<= roll 70) battle #_(with-meta 'battle {:name "battle"})
    (<= roll 80) lost #_(with-meta 'lost {:name "lost"})
    (<= roll 90) plague #_(with-meta 'plague {:name "plague"})
    (<= roll 100) dragon-attack #_(with-meta 'dragon-attack {:name "dragon-attack"})))

(defn move [player destination]
  (let [roll (rand-nth (range 1 101))
        move-fn (roll-action roll)
        result (if (= 'dragon-attack move-fn)
                 (move-fn player {:warriors 0 :gold 0} destination)
                 (move-fn player destination))
        _ (log/info "move-result" result)
        updated-player (cond-> player
                         (:warriors result) (assoc :warriors (:warriors result))
                         (:gold result) (assoc :gold (:gold result))
                         (:reason result) (assoc :reason (:reason result))
                         :always (assoc :move-result (:move-result result) :current-territory (:current-territory result)))]
    (log/info "updated-player" updated-player)
    updated-player))
