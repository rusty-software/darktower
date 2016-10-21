(ns darktower.server.game.bazaar
  (:require [darktower.server.game.main :as main]))

(def min-price
  {:warrior 4
   :food 1
   :beast 15
   :scout 15
   :healer 15})

(defn init [player]
  (cond-> {:current-item :warrior
           :warrior (+ (main/roll-dn 8) 3)
           :food 1}

    (not (:beast player))
    (assoc :beast (+ (main/roll-dn 12) 14))

    (not (:scout player))
    (assoc :scout (+ (main/roll-dn 12) 14))

    (not (:healer player))
    (assoc :healer (+ (main/roll-dn 12) 14))))

(defn next-item [bazaar]
  (let [items (vec (rest (keys bazaar)))
        current-item-idx (.indexOf items (:current-item bazaar))]
    (if (= current-item-idx (dec (count items)))
      (assoc bazaar :current-item (get items 0))
      (assoc bazaar :current-item (get items (inc current-item-idx))))))

(defn haggle [bazaar]
  (let [roll (main/roll-dn 2)
        new-price (dec (get bazaar (:current-item bazaar)))]
    (if (or (= 1 roll)
            (< new-price (get min-price (:current-item bazaar))))
      (assoc bazaar :closed? true)
      (assoc bazaar (:current-item bazaar) new-price))))