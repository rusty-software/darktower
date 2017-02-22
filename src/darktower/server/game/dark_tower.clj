(ns darktower.server.game.dark-tower
  (:require [taoensso.timbre :as log]))

(defn init []
  {:current-key :brass-key
   :remaining-keys [:brass-key :silver-key :gold-key]})

(defn riddle-of-the-keys []
  (shuffle [:brass-key :silver-key :gold-key]))

(defn brigands [difficulty]
  (case difficulty
    1 (rand-nth (range 17 33))
    2 (rand-nth (range 32 65))
    (rand-nth (range 17 65))))

(defn next-key [keys current-key]
  (let [current-key-idx (.indexOf keys current-key)]
    (if (= current-key-idx (dec (count keys)))
      (get keys 0)
      (get keys (inc current-key-idx)))))

(defn key-fits? [key current-lock]
  (= key current-lock))

(defn current-lock [riddle-of-the-keys remaining-keys]
  (if (= 3 (count remaining-keys))
    (first riddle-of-the-keys)
    (second riddle-of-the-keys)))

(defn try-key [riddle-of-the-keys remaining-keys key]
  (let [current-lock (current-lock riddle-of-the-keys remaining-keys)
        key-fits? (key-fits? key current-lock)
        remaining-keys (if key-fits?
                      (remove #(= current-lock %) remaining-keys)
                      remaining-keys)]
    (cond
      (and key-fits?
           (> (count remaining-keys) 1))
      {:result :successful-try :remaining-keys remaining-keys}

      key-fits?
      {:result :dark-tower-battle}

      :else
      {:result :wrong-key :remaining-keys remaining-keys})))
