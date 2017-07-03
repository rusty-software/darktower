(ns darktower.views.buttons
  (:require [darktower.communication :as communication]))

(defn end-turn-button []
  [:button
   {:id "btn-end-turn"
    :class "button end-turn"
    :on-click #(communication/end-turn)}
   "End Turn"])

(defn fight-button []
  [:button
   {:id "btn-fight"
    :class "button fight"
    :on-click #(communication/fight)}
   "Fight"])

(defn flee-button []
  [:button
   {:id "btn-flee"
    :class "button flee"
    :on-click #(communication/flee)}
   "Flee"])

(defn next-button []
  [:button
   {:id "btn-next"
    :class "button next"
    :on-click #(communication/next-item)}
   "Next"])

(defn buy-button []
  [:button
   {:id "btn-buy"
    :class "button buy"
    :on-click #(communication/buy-item)}
   "Buy"])

(defn haggle-button []
  [:button
   {:id "btn-haggle"
    :class "button haggle"
    :on-click #(communication/haggle)}
   "Haggle"])

(defn curse-button []
  [:button
   {:id "btn-yes"
    :class "button yes"
    :on-click #(communication/curse)}
   "Curse"])

(defn no-button []
  [:button
   {:id "btn-no"
    :class "button no"
    :on-click #(communication/next-key)}
   "No"])

(defn yes-button []
  [:button
   {:id "btn-yes"
    :class "button yes"
    :on-click #(communication/try-key)}
   "Yes"])

(defn display-buttons [buttons]
  [:div (for [button buttons]
          ^{:key (str "btn-" button)}
          [button])])

(defn display-battle-buttons []
  (display-buttons [fight-button flee-button]))

(defn display-bazaar-no-buy-buttons []
  (display-buttons [next-button haggle-button end-turn-button]))

(defn display-bazaar-buttons []
  (display-buttons [next-button buy-button haggle-button end-turn-button]))

(defn display-wizard-buttons []
  (display-buttons [curse-button end-turn-button]))

(defn display-riddle-buttons []
  (display-buttons [no-button yes-button end-turn-button]))

(defn display-end-button []
  (display-buttons [end-turn-button]))
