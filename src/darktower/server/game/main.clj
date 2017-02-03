(ns darktower.server.game.main)

(defn roll-d100 []
  (rand-nth (range 1 101)))

(defn roll-dn [n]
  (rand-nth (range 1 (inc n))))

(defn won-the-toss? []
  (= 2 (roll-dn 2)))
