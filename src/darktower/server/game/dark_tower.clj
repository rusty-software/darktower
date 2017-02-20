(ns darktower.server.game.dark-tower
  (:require [taoensso.timbre :as log]))

(defn init []
  {:current-key :brass-key
   :remaining-keys [:brass-key :silver-key :gold-key]})

(defn riddle-of-the-keys []
  (shuffle [:brass-key :silver-key :gold-key]))

(defn brigands [difficulty]
  (log/info "difficulty" difficulty)
  (case difficulty
    1 (rand-nth (range 17 33))
    2 (rand-nth (range 32 65))
    (rand-nth (range 17 65))))

(defn next-key [keys current-key]
  (let [current-item-idx (.indexOf keys current-key)]
    (log/info "current-item-idx" current-item-idx)
    (if (= current-item-idx (dec (count keys)))
      (get keys 0)
      (get keys (inc current-item-idx)))))

(defn- key-fits [key current-lock]
  (= key current-lock))

(defn try-key [remaining-keys key]
  (let [current-lock (first remaining-keys)
        other-locks (rest remaining-keys)]
    (cond
      (and (key-fits key current-lock)
           (> (count other-locks) 1))
      {:result :successful-try :remaining-keys (rest remaining-keys)}

      (key-fits key current-lock)
      {:result :dark-tower-battle}

      :else
      {:result :wrong-key :remaining-keys remaining-keys})))
