(ns darktower.model
  (:require
    [reagent.core :as reagent]))

(defonce game-state (reagent/atom {}))

(defn update-player-name! [name]
  (swap! game-state assoc :player-name name))

(defn update-difficulty! [difficulty]
  (swap! game-state assoc :difficulty difficulty))

(defn update-joining-game-token! [token]
  (swap! game-state assoc :joining-game-token token))

(defn update-uid! [uid]
  (swap! game-state assoc :uid uid))

(defn update-server-state! [server-state]
  (swap! game-state assoc :server-state server-state))
