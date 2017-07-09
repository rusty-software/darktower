(ns darktower.server.game
  (:require [taoensso.timbre :as log]
            [schema.core :as s]
            [darktower.server.board :as board]
            [darktower.server.schema :as schema]
            [darktower.server.game.main :as main]
            [darktower.server.game.treasure :as treasure]
            [darktower.server.game.bazaar :as bazaar]
            [darktower.server.game.dark-tower :as dark-tower]
            [clojure.set :as set]))

(s/defn initialize-player
  ([player] :- schema/Player
    (initialize-player false player))
  ([multiplayer? player] :- schema/Player
    (assoc player :current-territory {:kingdom (:kingdom player) :row 5 :idx 3}
                  :warriors 10
                  :gold 30
                  :food 25
                  :move-count 0
                  :multiplayer? multiplayer?
                  :scout false
                  :healer false
                  :beast false
                  :brass-key false
                  :silver-key false
                  :gold-key false
                  :pegasus false
                  :sword false)))

(defn initialize-game [players difficulty]
  (let [multiplayer? (> (count players) 1)
        init-players (map (partial initialize-player multiplayer?) players)]
    {:players init-players
     :player-order (vec (map :uid init-players))
     :current-player (:uid (first init-players))
     :dragon-hoard {:warriors 0 :gold 0}
     :riddle-of-the-keys (dark-tower/riddle-of-the-keys)
     :dark-tower-brigands (dark-tower/brigands difficulty)}))

(defn normalized-territory [{:keys [kingdom row idx type]}]
  (cond-> {:kingdom kingdom}

          (not (nil? row))
          (assoc :row row)

          (not (nil? idx))
          (assoc :idx idx)

          (not (nil? type))
          (assoc :type type)))

(defn requires-key? [player destination]
  (or (= :dark-tower (:type destination))
      (and
        (= :frontier (:type destination))
        (not= (:kingdom player) (:kingdom destination)))))

;; TODO: (maybe) cannot re-enter dark tower after fleeing until visiting the bazaar or home citadel
(defn valid-move [{:keys [current-territory encounter-result kingdom pegasus] :as player} destination]
  (let [neighbors (board/neighbors-for current-territory)]
    (cond
      (= :dark-tower-won encounter-result)
      {:valid? false :message "The game is over! Start a new one if you want to keep moving!" :encounter-result :dark-tower-won}

      (#{:battle :fighting-won-round :fighting-lost-round} encounter-result)
      {:valid? false :message "Cannot move while in battle!" :encounter-result encounter-result}

      (and
        (requires-key? player destination)
        (not (treasure/has-key? player destination)))
      {:valid? false :message "Key missing!" :encounter-result :invalid-move}

      (and
        (= :citadel (board/type-for destination))
        (not= (:kingdom destination) kingdom))
      {:valid? false :message "Cannot enter foreign citadel!" :encounter-result :invalid-move}

      (some #{destination} neighbors)
      {:valid? true}

      (and
        (= (:kingdom destination) (:kingdom current-territory))
        pegasus)
      {:valid? true :pegasus-required? true}

      :else
      {:valid? false :message "Destination must be adjacent to your current territory!" :encounter-result :invalid-move})))

(defn safe-move [player]
  {:player (assoc player :encounter-result :safe-move)})

(defn brigands-for [{:keys [warriors]}]
  (let [delta (/ warriors 4)
        min-brigands (max 1 (min 99 (int (* 3 delta))))
        max-brigands (max 3 (int (+ warriors delta)))]
    (rand-nth (range min-brigands (inc max-brigands)))))

(defn battle
  ([player brigands]
   {:player (assoc player :encounter-result :battle :brigands brigands)})
  ([player]
   (battle player (brigands-for player))))

(defn battle-if-possible [player]
  (if (> (:warriors player) 1)
    (battle player)
    (safe-move player)))

(defn lost [{:keys [scout last-territory] :as player}]
  (if scout
    {:player (-> player
                 (assoc :encounter-result :lost-scout :extra-turn true)
                 (update :move-count dec))}
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

(defn curse [cursed-player cursor]
  (let [{cursed-player-warriors :warriors cursed-player-gold :gold} cursed-player
        {cursor-warriors :warriors cursor-gold :gold} cursor
        warriors-taken (if (= 1 cursed-player-warriors) 0 (max 1 (int (* 0.25 cursed-player-warriors))))
        gold-taken (if (= 1 cursed-player-gold) 0 (max 1 (int (* 0.25 cursed-player-gold))))]
    {:cursed-player (assoc cursed-player :warriors (- cursed-player-warriors warriors-taken)
                                         :gold (- cursed-player-gold gold-taken)
                                         :encounter-result :cursed)
     :cursor (assoc cursor :warriors (min 99 (+ cursor-warriors warriors-taken))
                           :gold (min 99 (+ cursor-gold gold-taken))
                           :encounter-result :cursor)}))

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

(defn fight [{:keys [warriors brigands gold beast at-dark-tower?] :as player}]
  (let [warriors-win? (>= (winning-chance warriors brigands) (main/roll-d100))]
    (cond
      (and warriors-win? (>= 1 brigands) at-dark-tower?)
      {:player (-> player
                   (assoc :encounter-result :dark-tower-won)
                   (dissoc :brigands))}

      (and warriors-win? (>= 1 brigands))
      {:player (-> player
                   (merge (treasure/treasure player))
                   (assoc :encounter-result :fighting-won)
                   (dissoc :brigands))}

      (and warriors-win? at-dark-tower?)
      {:player (assoc player :encounter-result :dark-tower-won-round
                             :brigands (int (/ brigands 2)))}

      warriors-win?
      {:player (assoc player :encounter-result :fighting-won-round
                             :brigands (int (/ brigands 2)))}

      (and (= 2 warriors) at-dark-tower?)
      {:player (assoc player :encounter-result :dark-tower-lost
                             :warriors 1
                             :gold (treasure/adjust-gold 1 gold beast))}

      (= 2 warriors)
      {:player (assoc player :encounter-result :fighting-lost
                                            :warriors 1
                                            :gold (treasure/adjust-gold 1 gold beast))}

      at-dark-tower?
      {:player (assoc player :encounter-result :dark-tower-lost-round
                             :warriors (dec warriors)
                             :gold (treasure/adjust-gold (dec warriors) gold beast))}

      :else
      {:player (assoc player :encounter-result :fighting-lost-round
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

(defmulti encounter-location :type)

(defmethod encounter-location :default [{:keys [type player]}]
  (log/info "default location handler:" type)
  {:player player :encounter-result type})

(defmethod encounter-location :ruin [{:keys [player]}]
  (let [roll (main/roll-d100)]
    (cond
      (< 0 roll 21) (safe-move player)
      (< 20 roll 31) (-> player
                         (merge (treasure/treasure player))
                         (safe-move))
      (< 30 roll 101) (battle player)
      :else {:player player :encounter-result :unhandled})))

(defmethod encounter-location :tomb [params]
  (encounter-location (assoc params :type :ruin)))

(defn should-double-warriors? [{:keys [brass-key silver-key gold-key warriors last-location current-territory kingdom]}]
  (and brass-key silver-key gold-key
    (= kingdom (:kingdom current-territory))
    (< 4 warriors 25)
    (not (#{:sanctuary :citadel} last-location))))

(defmethod encounter-location :sanctuary [{:keys [player]}]
  (let [updated-player (cond-> (assoc player :awarded #{})
                               (should-double-warriors? player)
                               (->
                                 (update :warriors * 2)
                                 (update :awarded set/union #{:warriors}))

                               (< (:warriors player) 5)
                               (->
                                 (update :warriors + (main/roll-dn 4) 4)
                                 (update :awarded set/union #{:warriors}))

                               (< (:gold player) 8)
                               (->
                                 (update :gold + (main/roll-dn 7) 9)
                                 (update :awarded set/union #{:gold}))

                               (< (:food player) 6)
                               (->
                                 (update :food + (main/roll-dn 7) 9)
                                 (update :awarded set/union #{:food}))

                               :always
                               (assoc :encounter-result :sanctuary))]
    {:player updated-player}))

(defmethod encounter-location :citadel [params]
  (encounter-location (assoc params :type :sanctuary)))

(defmethod encounter-location :bazaar [{:keys [player]}]
  {:player (assoc player :encounter-result :bazaar :bazaar-inventory (bazaar/init player))})

(defmethod encounter-location :dark-tower [{:keys [player]}]
  {:player (assoc player :encounter-result :dark-tower :dark-tower-status (dark-tower/init))})

(defn encounter [player dragon-hoard]
  (let [territory-type (board/type-for (dissoc (:current-territory player) :kingdom))
        player (-> player
                   (dissoc :awarded)
                   (update :move-count inc))]
    (if (= :territory territory-type)
      (encounter-territory player dragon-hoard)
      (encounter-location {:type territory-type :player player}))))

;; TODO: starving
(defn feed [{:keys [warriors food]}]
  (if (= warriors 99)
    {:food (max 0 (- food 7))}
    {:food (max 0 (- food (inc (int (/ warriors 15.1)))))}))

(defn flee [{:keys [warriors at-dark-tower?] :as player}]
  {:player (assoc player
             :encounter-result (if at-dark-tower? :dark-tower-fled :fled)
             :warriors (max 1 (- warriors 1)))})

(defn- bazaar-interaction [player interaction]
  (assoc player :encounter-result :bazaar :bazaar-inventory (interaction (:bazaar-inventory player))))

(defn- funds-check [player]
  (let [bazaar (:bazaar-inventory player)
        current-item (:current-item bazaar)
        player (dissoc player :insufficient-funds?)]
    (if (< (:gold player) (get bazaar current-item))
      (assoc player :insufficient-funds? true)
      player)))

(defn next-item [player]
  (let [player (-> player
                   (bazaar-interaction bazaar/next-item)
                   (funds-check))]
    {:player player}))

(defn haggle [player]
  (let [player (-> player
                   (bazaar-interaction bazaar/haggle)
                   (funds-check))]
    {:player player}))

(defn- add-item [val item]
  (cond
    (#{:food :warriors} item) (min 99 (inc val))
    (#{:beast :scout :healer} item) true))

(defn buy-item [player]
  (let [bazaar (:bazaar-inventory player)
        current-item (:current-item bazaar)
        item-cost (get bazaar current-item)
        player-checked (funds-check player)]
    (if (not (:insufficient-funds? player-checked))
      (let [player (-> player
                       (update :gold - item-cost)
                       (update current-item add-item current-item)
                       (funds-check))]
        {:player player})
      {:player player-checked})))

(defn next-key [player]
  (let [{:keys [current-key remaining-keys] :as dark-tower-status} (:dark-tower-status player)
        current-key (dark-tower/next-key remaining-keys current-key)
        dark-tower-status (assoc dark-tower-status :current-key current-key)
        player (assoc player :dark-tower-status dark-tower-status)]
    {:player player}))

(defn try-key [riddle-of-the-keys player]
  (let [{:keys [current-key remaining-keys] :as dark-tower-status} (:dark-tower-status player)
        {:keys [result remaining-keys]} (dark-tower/try-key riddle-of-the-keys remaining-keys current-key)]
    (if (= :dark-tower-battle result)
      (battle (assoc player :at-dark-tower? true) 6)        ;;TODO: replace hard-coded brigand count with game's brigand count
      (let [dark-tower-status (assoc dark-tower-status :remaining-keys remaining-keys :current-key (first remaining-keys))
            player (assoc player :dark-tower-status dark-tower-status)]
        {:player player}))))
