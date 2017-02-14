(ns darktower.server.game.dark-tower)

(defn next-key [keys current-key]
  (let [current-item-idx (.indexOf keys current-key)]
    (if (= current-item-idx (dec (count keys)))
      (get keys 0)
      (get keys (inc current-item-idx)))))
