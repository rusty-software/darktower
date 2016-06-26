(ns darktower.server.model)

(defonce app-state
  (atom {}))

(defn game-token []
  (Integer/toString (rand-int (Math/pow 36 6)) 36))

(def kingdoms [:arisilon :brynthia :durnin :zenon])

(defn initialize-game [app-state uid token name]
  (let [kingdom (rand-nth kingdoms)
        remaining-kingdoms (remove #{kingdom} kingdoms)]
    (assoc app-state token {:token token
                            :initialized-by uid
                            :remaining-colors remaining-kingdoms
                            :players [{:uid uid
                                       :name name
                                       :kingdom kingdom}]})))

(defn initialize-game! [uid token name]
  (swap! app-state initialize-game uid token name))
