(ns darktower.server.test-helpers)

(def player
  {:uid "15"
   :name "rusty"
   :kingdom :arisilon})

(defn top-row-edge [player kingdom]
  (assoc player :current-territory {:kingdom kingdom :row 1 :idx 2}))

