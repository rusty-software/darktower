(ns darktower.views.displays
  (:require [darktower.views.buttons :as buttons]
            [darktower.model :as model]))

(defn battle-display [warriors brigands]
  [:div
   [:span (str warriors " warriors; " brigands " brigands")]
   (buttons/display-battle-buttons)])

;; TODO: Remove buttons after item is bought
(defn bazaar-display [bazaar-inventory insufficient-funds?]
  (if (not (:closed? bazaar-inventory))
    [:div
     [:span (str "Cost: " (get bazaar-inventory (:current-item bazaar-inventory)))]
     (if insufficient-funds?
       (buttons/display-bazaar-no-buy-buttons)
       (buttons/display-bazaar-buttons))]
    [:div
     (buttons/display-end-button)]))

(defn player-select [current-player]
  [:div
   "Cursing:"
   [:select {:on-change #(model/select-player! (.. % -target -value))}
    (for [{:keys [name uid]} (get-in @model/game-state [:server-state :players])]
      (when (not= uid (:uid current-player))
        ^{:key (str "select-" uid)}
        [:option {:value uid} name]))]])

(defn player-name-text [current-player]
  (let [other-player (first (filter #(not= (:uid current-player) (:uid %)) (get-in @model/game-state [:server-state :players])))]
    (model/select-player! (:uid other-player))
    [:div (str "Cursing: " (:name other-player))]))

(defn wizard-display [current-player]
  [:div
   (if (> (get-in @model/game-state [:server-state :players]) 2)
     (player-select current-player)
     (player-name-text current-player))
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
   [:text "Your turn. (reminder: go clockwise)"]
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
                             (and (= :safe-move encounter-result) (:wizard current-player)) :wizard
                             :else encounter-result)
                images (get-in encounter-result-specs [images-key :images])]
            (for [image images]
              (do
                ^{:key (str "img-" image)}
                [:img {:src image
                       :style {:margin "5px"}}])))
          (cond
            (or (= awarded :wizard) (and (= :safe-move encounter-result) (:wizard current-player)))
            (wizard-display current-player)

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
