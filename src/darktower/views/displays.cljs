(ns darktower.views.displays
  (:require [darktower.views.buttons :as buttons]))

(defn battle-display [warriors brigands]
  [:div
   [:span (str warriors " warriors; " brigands " brigands")]
   (buttons/display-battle-buttons)])

(defn bazaar-display [bazaar-inventory insufficient-funds?]
  (if (not (:closed? bazaar-inventory))
    [:div
     [:span (str "Cost: " (get bazaar-inventory (:current-item bazaar-inventory)))]
     (if insufficient-funds?
       (buttons/display-bazaar-no-buy-buttons)
       (buttons/display-bazaar-buttons))]
    [:div
     (buttons/display-end-button)]))

(defn player-select []
  ;; TODO: onchange handler, populate from players in atom
  [:div
   "Cursing:"
   [:select
    [:option {:value 1} "fake 1"]
    [:option {:value 2} "fake 2"]
    [:option {:value 3} "fake 3"]]])

(defn wizard-display []
  [:div
   [:span "Click End Turn to curse no one."]
   (player-select)
   (buttons/display-wizard-buttons)])

(defn dark-tower-display [dark-tower-status]
  [:div
   [:span "Does this key fit the lock?"]
   (buttons/display-riddle-buttons)])

(defn message-display [message]
  [:div
   [:span message]
   (buttons/display-end-button)])

(defn current-player-display [encounter-result-specs current-player]
  [:div
   [:text "Your turn."]
   [:br]
   [:div
    {:class "dt-display"}
    (let [{:keys [encounter-result awarded bazaar-inventory dark-tower-status insufficient-funds? warriors brigands message]} current-player]
      [:div
       {:class "dt-image"}
       (when encounter-result
         [:div
          (let [images-key (cond
                             awarded #{encounter-result awarded}
                             (:closed? bazaar-inventory) :bazaar-closed
                             (= :bazaar encounter-result) (:current-item bazaar-inventory)
                             (= :dark-tower encounter-result) (:current-key dark-tower-status)
                             (= "Key missing!" message) :key-missing
                             :else encounter-result)
                images (get-in encounter-result-specs [images-key :images])]
            (for [image images]
              (do
                ^{:key (str "img-" image)}
                [:img {:src image
                       :style {:margin "5px"}}])))
          (cond
            (= awarded :wizard)
            (wizard-display)

            (#{:battle :fighting-won-round :fighting-lost-round :dark-tower-won-round :dark-tower-lost-round} encounter-result)
            (battle-display warriors brigands)

            (= :bazaar encounter-result)
            (bazaar-display bazaar-inventory insufficient-funds?)

            (= :dark-tower encounter-result)
            (dark-tower-display dark-tower-status)

            message
            (message-display message)

            :else
            (buttons/display-end-button))])])]])

(defn other-player-display [current-player-name]
  [:div
   [:text (str current-player-name "'s turn.")]])

(defn current-player-victory-display [img-src {:keys [move-count]}]
  [:div
   [:text (str "VICTORY! Congratulations! You won in " move-count " moves. Record this feat for future reference!")]
   [:br]
   [:div
    [:img
     {:src img-src}]]])

(defn other-player-victory-display [{:keys [name move-count]}]
  [:div
   [:text (str "The game has been won by " name " in " move-count " moves. Congratulate them on a well-earned(?) victory!")]])
