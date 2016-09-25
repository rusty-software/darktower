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

(def offset-key [true :brass-key :silver-key :gold-key])

(defn has-key? [player destination]
  (let [offset (board/kingdom-offset (:kingdom player) (:kingdom destination))
        key (get offset-key offset)]
    (if (zero? offset)
      true
      (get player key))))

;; TODO: players cannot move onto foreign citadels
(defn valid-move [player destination]
  (let [current-territory (:current-territory player)
        neighbors (board/neighbors-for current-territory)
        requires-key? (not= (:kingdom player) (:kingdom destination))]
    (cond
      (and
        requires-key?
        (not (has-key? player destination)))
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
        min-brigands (min 99 (int (* 3 delta)))
        max-brigands (max 3 (int (+ warriors delta)))]
    (rand-nth (range min-brigands (inc max-brigands)))))

(defn battle [player]
  {:player (assoc player :encounter-result :battle :brigands (brigands-for player))})

(defn lost [{:keys [scout last-territory] :as player}]
  (if scout
    {:player (assoc player :encounter-result :lost-scout :extra-turn true)}
    {:player (assoc player :encounter-result :lost :current-territory last-territory)}))

(defn plague [{:keys [healer warriors] :as player}]
  (if healer
    {:player (assoc player :encounter-result :plague-healer :warriors (min 99 (+ warriors 2)))}
    {:player (assoc player :encounter-result :plague :warriors (max 1 (- warriors 2)))}))

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

(defn roll-result [roll]
  (cond
    (>= 50 roll) :safe-move
    (>= 70 roll) :battle
    (>= 82 roll) :lost
    (>= 94 roll) :plague
    (>= 100 roll) :dragon-attack))

(defn roll-100 []
  (rand-nth (range 1 101)))

(defn winning-chance [warriors brigands]
  (if (> warriors brigands)
    (* 100 (- 0.75 (/ brigands (* 4 warriors))))
    (* 100 (+ 0.25 (/ warriors (* 4 brigands))))))

(defn encounter-territory [player dragon-hoard]
  (let [roll-action (roll-result (roll-100))]
    (cond
      (= :safe-move roll-action) (safe-move player)
      (= :battle roll-action) (battle player)
      (= :lost roll-action) (lost player)
      (= :plague roll-action) (plague player)
      (= :dragon-attack roll-action) (dragon-attack player dragon-hoard))))

(defn encounter-location [player location]
  (log/info "encountering location" location)
  {:player player :encounter-result location})

(defn encounter [player dragon-hoard]
  (let [territory-type (board/type-for (dissoc (:current-territory player) :kingdom))]
    (if (= :territory territory-type)
      (encounter-territory player dragon-hoard)
      (encounter-location player territory-type))))

(defn feed [{:keys [warriors food]}]
  (if (= warriors 99)
    {:food (max 0 (- food 7))}
    {:food (max 0 (- food (inc (int (/ warriors 15.1)))))}))

(defn flee [{:keys [warriors] :as player}]
  {:player (assoc player :encounter-result :fled :warriors (max 1 (- warriors 1)))})

(defn treasure-gold [current-gold]
  (min 99 (+ current-gold 10 (int (* 11 (rand))))))

(defn can-award-key? [player]
  (let [offset (board/kingdom-offset (:kingdom player) (get-in player [:current-territory :kingdom]))
        key (get offset-key offset)]
    (if (zero? offset)
      false
      (not (get player key)))))

(defn award-key [player]
  (let [offset (board/kingdom-offset (:kingdom player) (get-in player [:current-territory :kingdom]))
        key (get offset-key offset)]
    (if (zero? offset)
      player
      (assoc player key true))))

;; TODO: wizard handler

(defn can-receive-treasure?
  ([player]
    (can-receive-treasure? player nil))
  ([player multiplayer?]
    (or (< (:gold player) 99)
      (not (has-key? player (:current-territory player)))
      (not (:pegasus player))
      (not (:sword player))
      (and multiplayer? (not (:wizard player))))))

(defn treasure-types
  ([]
    (treasure-types nil))
  ([multiplayer?]
   (if multiplayer?
     #{:gold :key :pegasus :sword :wizard}
     #{:gold :key :pegasus :sword})))

(defn treasure-type [roll multiplayer?]
  (cond
    (<= roll 30) :gold
    (<= 31 roll 50) :key
    (<= 51 roll 70) :pegasus
    (<= 71 roll 85) :sword
    (and (<= 86 roll 100) multiplayer?) :wizard
    :else :gold))

(defn tried-everything? [tried multiplayer?]
  (not (seq (clojure.set/difference (treasure-types multiplayer?) tried))))

(defn can-award? [treasure player]
  (cond
    (= :gold treasure) (< (:gold player) 99)
    (= :key treasure) (can-award-key? player)

    :else false))

(defn award-treasure [treasure player]
  player)

(defn treasure
  ([player]
    (treasure player nil))
  ([player multiplayer?]
   (if (can-receive-treasure? player)
     (loop [treasure-to-try (treasure-type (roll-100) multiplayer?)
            tried #{}
            multiplayer? multiplayer?]
       (cond
         (tried-everything? tried multiplayer?)
         player

         (can-award? treasure-to-try player)
         (award-treasure treasure-to-try player)

         :else
         (recur (treasure-to-try (roll-100)) (conj tried treasure-to-try) multiplayer?)))

     #_(let [roll (roll-100)]
       (cond
         (<= roll 30)
         (update player :gold treasure-gold)

         (and (<= 31 roll 50) (not (has-key? player (:current-territory player))))
         (award-key player)

         (<= 51 roll 70)
         (assoc player :pegasus true)

         (<= 71 roll 85)
         (assoc player :sword true)

         (and (<= 85 roll 100) multiplayer?)
         (assoc player :wizard true)

         :else player))
     player)))

(defn fight [{:keys [warriors brigands] :as player}]
  (let [warriors-win? (>= (winning-chance warriors brigands) (roll-100))]
    (cond
      warriors-win? {:player (assoc player :encounter-result :fighting-won-round
                                           :brigands (/ brigands 2))}
      (= 2 warriors) {:player (assoc player :encounter-result :fighting-lost
                                            :warriors 1)}
      :else {:player (assoc player :encounter-result :fighting-lost-round
                                   :warriors (dec warriors))})))
