(ns darktower.server.game.dark-tower)

(defn next-key [keys current-key]
  (let [current-item-idx (.indexOf keys current-key)]
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
