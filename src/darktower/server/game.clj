(ns darktower.server.game
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [darktower.server.board :as board]
            [darktower.server.schema :as schema]
            [darktower.server.game.main :as main]
            [darktower.server.game.treasure :as treasure]))

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

(defn requires-key? [player destination]
  (and
    (= :frontier (:type destination))
    (not= (:kingdom player) (:kingdom destination))))

;; TODO: players cannot move onto foreign citadels
(defn valid-move [player destination]
  (let [current-territory (:current-territory player)
        neighbors (board/neighbors-for current-territory)]
    (cond
      (and
        (requires-key? player destination)
        (not (treasure/has-key? player destination)))
      {:valid? false :message "Key missing!"}

      (some #{destination} neighbors)
      {:valid? true}

      (and
        (= (:kingdom destination) (:kingdom current-territory))
        (:pegasus player))
      {:valid? true :pegasus-required? true}

      :else
      {:valid? false :message "Destination must be adjacent to your current territory!"})))

(defn safe-move [player]
  {:player (assoc player :encounter-result :safe-move)})

(defn brigands-for [{:keys [warriors]}]
  (let [delta (/ warriors 4)
        min-brigands (max 1 (min 99 (int (* 3 delta))))
        max-brigands (max 3 (int (+ warriors delta)))]
    (rand-nth (range min-brigands (inc max-brigands)))))

(defn battle [player]
  {:player (assoc player :encounter-result :battle :brigands (brigands-for player))})

(defn battle-if-possible [player]
  (if (> (:warriors player) 1)
    (battle player)
    (safe-move player)))

(defn lost [{:keys [scout last-territory] :as player}]
  (if scout
    {:player (assoc player :encounter-result :lost-scout :extra-turn true)}
    {:player (assoc player :encounter-result :lost :current-territory last-territory)}))

(defn plague [{:keys [healer warriors gold beast] :as player}]
  (let [result (if healer :plague-healer :plague)
        warriors-after-plague (if healer
                                (min 99 (+ warriors 2))
                                (max 1 (- warriors 2)))
        gold-after-plague (treasure/adjust-gold warriors-after-plague gold beast)]
    {:player (assoc player :encounter-result result :warriors warriors-after-plague :gold gold-after-plague)}))

(defn dragon-attack [{:keys [sword warriors gold] :as player} dragon-hoard]
  (let [{dragon-warriors :warriors dragon-gold :gold} dragon-hoard]
    (if sword
      {:player (assoc player :encounter-result :dragon-attack-sword
                             :warriors (min 99 (+ warriors dragon-warriors))
                             :gold (min 99 (+ gold dragon-gold))
                             :sword false)
       :dragon-hoard {:warriors 0 :gold 0}}
      (let [warriors-taken (if (= 1 warriors) 0 (max 1 (int (* 0.25 warriors))))
            gold-taken (if (= 1 gold) 0 (max 1 (int (* 0.25 gold))))]
        {:player (assoc player :encounter-result :dragon-attack
                               :warriors (- warriors warriors-taken)
                               :gold (- gold gold-taken))
         :dragon-hoard {:warriors (+ dragon-warriors warriors-taken) :gold (+ dragon-gold gold-taken)}}))))

(defn encounter-roll-result [roll]
  (cond
    (>= 50 roll) :safe-move
    (>= 70 roll) :battle
    (>= 82 roll) :lost
    (>= 94 roll) :plague
    (>= 100 roll) :dragon-attack))

(defn winning-chance [warriors brigands]
  (if (> warriors brigands)
    (* 100 (- 0.75 (/ brigands (* 4 warriors))))
    (* 100 (+ 0.25 (/ warriors (* 4 brigands))))))

(defn fight [{:keys [warriors brigands gold beast] :as player}]
  (let [warriors-win? (>= (winning-chance warriors brigands) (main/roll-d100))]
    (cond
      (and warriors-win? (>= 1 brigands)) {:player (-> player
                                                       (merge (treasure/treasure player))
                                                       (assoc :encounter-result :fighting-won)
                                                       (dissoc :brigands))}
      warriors-win? {:player (assoc player :encounter-result :fighting-won-round
                                           :brigands (int (/ brigands 2)))}
      (= 2 warriors) {:player (assoc player :encounter-result :fighting-lost
                                            :warriors 1
                                            :gold (treasure/adjust-gold 1 gold beast))}
      :else {:player (assoc player :encounter-result :fighting-lost-round
                                   :warriors (dec warriors)
                                   :gold (treasure/adjust-gold (dec warriors) gold beast))})))

(defn encounter-territory [player dragon-hoard]
  (let [roll-action (encounter-roll-result (main/roll-d100))]
    (case roll-action
      :safe-move (safe-move player)
      :battle (battle-if-possible player)
      :lost (lost player)
      :plague (plague player)
      :dragon-attack (dragon-attack player dragon-hoard)
      (safe-move player))))

;; TODO: set last-location type
(defmulti encounter-location :type)

(defmethod encounter-location :default [{:keys [type player]}]
  (log/info "default location handler:" type)
  {:player player :encounter-result type})

(defmethod encounter-location :ruin [{:keys [player]}]
  (let [roll (main/roll-d100)]
    (cond
      (< 0 roll 21) (safe-move player)
      (< 20 roll 31) (-> player (merge (treasure/treasure player)) (safe-move))
      (< 30 roll 101) (battle player)
      :else {:player player :encounter-result :unhandled})))

(defmethod encounter-location :tomb [params]
  (encounter-location (assoc params :type :ruin)))

(defn should-double-warriors? [{:keys [brass-key silver-key gold-key warriors last-location]}]
  (and brass-key silver-key gold-key
    (< 4 warriors 25)
    (not (#{:sanctuary :citadel} last-location))))

(defmethod encounter-location :sanctuary [{:keys [player]}]
  (cond-> player
    (should-double-warriors? player)
    (update :warriors * 2)

    (< (:warriors player) 5)
    (update :warriors + (main/roll-dn 3) 5)

    (< (:gold player) 8)
    (update :gold + (main/roll-dn 6) 10)

    (< (:food player) 6)
    (update :food + (main/roll-dn 6) 10)))

(defmethod encounter-location :citadel [params]
  (encounter-location (assoc params :type :sanctuary)))

(defn encounter [player dragon-hoard]
  ;; TODO: implement/increment move count
  (let [territory-type (board/type-for (dissoc (:current-territory player) :kingdom))]
    (log/info "encounter territory type:" territory-type)
    (if (= :territory territory-type)
      (encounter-territory player dragon-hoard)
      (encounter-location {:type territory-type :player player}))))

(defn feed [{:keys [warriors food]}]
  (if (= warriors 99)
    {:food (max 0 (- food 7))}
    {:food (max 0 (- food (inc (int (/ warriors 15.1)))))}))

(defn flee [{:keys [warriors] :as player}]
  {:player (assoc player :encounter-result :fled :warriors (max 1 (- warriors 1)))})
