(ns darktower.server.game.treasure
  (:require [darktower.server.game.main :as main]
            [darktower.server.board :as board]))

(def offset-key [true :brass-key :silver-key :gold-key])

(defn has-key? [player destination]
  (let [offset (board/kingdom-offset (:kingdom player) (:kingdom destination))
        key (get offset-key offset)]
    (if (zero? offset)
      true
      (get player key))))

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

(defn adjust-gold [warriors gold beast]
  (let [beast-increase (if beast 50 0)]
    (min 99 (min (+ beast-increase (* 6 warriors)) gold))))

(defn can-award-key? [player]
  (let [offset (board/kingdom-offset (:kingdom player) (get-in player [:current-territory :kingdom]))
        key (get offset-key offset)]
    (if (zero? offset)
      false
      (not (get player key)))))

(defn can-award?
  ([treasure player]
   (can-award? treasure player nil))
  ([treasure {:keys [gold warriors pegasus sword beast wizard] :as player} multiplayer?]
   (case treasure
     :gold (< gold (adjust-gold warriors 99 beast))
     :key (can-award-key? player)
     :pegasus (not pegasus)
     :sword (not sword)
     :wizard (and (not wizard) multiplayer?)
     false)))

(defn treasure-gold [{:keys [gold warriors beast]}]
  (let [total-gold (min 99 (+ gold 10 (int (* 11 (rand)))))]
    (adjust-gold warriors total-gold beast)))

(defn award-key [player]
  (let [offset (board/kingdom-offset (:kingdom player) (get-in player [:current-territory :kingdom]))
        key (get offset-key offset)]
    (if (zero? offset)
      player
      (assoc player key true))))

(defn award-treasure [treasure player]
  (case treasure
    :gold (assoc player :gold (treasure-gold player))
    :key (award-key player)
    :pegasus (assoc player :pegasus true)
    :sword (assoc player :sword true)
    :wizard (assoc player :wizard true)
    player))

;; TODO: wizard handler
(defn treasure
  ([player]
   (treasure player nil))
  ([player multiplayer?]
   (if (can-receive-treasure? player multiplayer?)
     (loop [treasure-to-try (treasure-type (main/roll-d100) multiplayer?)
            tried #{}
            multiplayer? multiplayer?]
       (cond
         (tried-everything? tried multiplayer?)
         player

         (can-award? treasure-to-try player multiplayer?)
         (award-treasure treasure-to-try player)

         :else
         (recur (treasure-type (main/roll-d100) multiplayer?) (conj tried treasure-to-try) multiplayer?)))
     player)))
