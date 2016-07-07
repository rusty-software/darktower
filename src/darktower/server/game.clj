(ns darktower.server.game
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [darktower.server.board :as board]
            [darktower.server.schema :as schema]))

(s/defn initialize-player [player] :- schema/Player
  (assoc player :current-territory {:kingdom (:kingdom player) :row 5 :idx 3}
                :warriors 10
                :gold 30
                :food 25
                :scout false
                :healer false
                :beast false
                :brass-key false
                :silver-key false
                :gold-key false
                :pegasus false
                :sword false))

(defn initialize-game [players]
  (let [init-players (map initialize-player players)]
    {:players init-players
     :player-order (vec (map :uid init-players))
     :current-player (:uid (first init-players))
     :dragon-hoard {:warriors 0 :gold 0}}))

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
      {:valid? false :message "Destination must be adjacent to your current territory!"})))

(defn safe-move [player]
  (log/info "safe-move")
  {:player player})

(defn battle [player]
  (log/info "battle not implemented")
  {:player player})

(defn lost [{:keys [scout last-territory] :as player}]
  (log/info "lost")
  (if scout
    {:player (assoc player :encounter-result :lost :extra-turn true)}
    {:player (assoc player :encounter-result :lost :current-territory last-territory)}))

(defn plague [{:keys [healer warriors] :as player}]
  (log/info "plague")
  (if healer
    {:player (assoc player :encounter-result :plague :warriors (min 99 (+ warriors 2)))}
    {:player (assoc player :encounter-result :plague :warriors (max 1 (- warriors 2)))}))

(defn dragon-attack [{:keys [sword warriors gold] :as player} dragon-hoard]
  (log/info "dragon-attack")
  (let [{dragon-warriors :warriors dragon-gold :gold} dragon-hoard]
    (if sword
      {:player (assoc player :encounter-result :dragon-attack
                             :warriors (min 99 (+ warriors dragon-warriors))
                             :gold (min 99 (+ gold dragon-gold)))
       :dragon-hoard {:warriors 0 :gold 0}}
      (let [warriors-taken (if (= 1 warriors) 0 (max 1 (int (* 0.25 warriors))))
            gold-taken (if (= 1 gold) 0 (max 1 (int (* 0.25 gold))))]
        {:player (assoc player :encounter-result :dragon-attack
                               :warriors (- warriors warriors-taken)
                               :gold (- gold gold-taken))
         :dragon-hoard {:warriors (+ dragon-warriors warriors-taken) :gold (+ dragon-gold gold-taken)}}))))

(defn roll-result [roll]
  (cond
    (>= 50 roll) :safe-move
    (>= 70 roll) :battle
    (>= 80 roll) :lost
    (>= 90 roll) :plague
    (>= 100 roll) :dragon-attack))

(defn encounter-territory [player dragon-hoard]
  (let [roll-action (roll-result (rand-nth (range 1 101)))]
    (cond
      (= :safe-move roll-action) (safe-move player)
      (= :battle roll-action) (battle player)
      (= :lost roll-action) (lost player)
      (= :plague roll-action) (plague player)
      (= :dragon-attack roll-action) (dragon-attack player dragon-hoard))))

(defn encounter-location [player location]
  (log/info "encountering location" location)
  {:player player})

(defn encounter [player dragon-hoard]
  (let [territory-type (board/type-for (dissoc (:current-territory player) :kingdom))]
    (if (= :territory territory-type)
      (encounter-territory player dragon-hoard)
      (encounter-location player territory-type))))

(defn feed [{:keys [warriors food]}]
  (if (= warriors 99)
    {:food (- food 7)}
    {:food (- food (inc (int (/ warriors 15.1))))}))
