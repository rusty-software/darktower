(ns darktower.view
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.pprint :as pprint]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [reagent.core :as r]
            [darktower.model :as model]
            [darktower.communication :as communication]
            [darktower.views.board :as board]))

(defn game-area []
  (board/main))

(defn player [uid]
  (let [players (get-in @model/game-state [:server-state :players])]
    (first (filter #(= uid (:uid %)) players))))

(defn current-player []
  (player (get-in @model/game-state [:server-state :current-player])))

(defn my-turn []
  (= (:uid @model/game-state) (:uid (current-player))))

(def ordered-keys [:warriors :gold :food :scout :healer :beast :brass-key :silver-key :gold-key :pegasus :sword])

(defn data-row-for [data-key players]
  ^{:key (str "tr-" data-key players)}
  [:tr
   [:td (name data-key)]
   (doall
     (for [player players]
       ^{:key (str "td-" player data-key)}
       [:td (get player data-key)]))])

(defn ordered-players []
  (for [player-id (get-in @model/game-state [:server-state :player-order])]
    (player player-id)))

(def encounter-result-specs
  {:lost {:images ["img/lost.jpg"]}
   :lost-scout {:images ["img/lost.jpg" "img/scout.jpg"]}
   :plague {:images ["img/plague.jpg"]}
   :plague-healer {:images ["img/plague.jpg" "img/healer.jpg"]}
   :dragon-attack {:images ["img/dragon.jpg"]}
   :dragon-attack-sword {:images ["img/dragon.jpg" "img/sword.jpg"]}
   :safe-move {:images ["img/victory.jpg"]}
   :battle {:images ["img/brigands.jpg"]}
   :fled {:images ["img/warriors.jpg"]}})

(defn display-buttons [buttons]
  [:div (for [button buttons] [button])])

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

(defn battle-display [warriors brigands]
  [:div
   [:span (str warriors " warriors; " brigands " brigands")]
   (display-buttons [fight-button flee-button])])

(defn player-area []
  [:div
   {:style
    {:display "inline-block"
     :width "490px"}}
   [:div {:id "darktower"}
    [:div {:id "darktower-display"}
     (if (my-turn)
       [:div
        [:text "Your turn."]
        [:br]
        [:div
         {:class "dt-display"}
         (let [{:keys [encounter-result warriors brigands]} (current-player)]
           [:div
            {:class "dt-image"}
            (when encounter-result
              [:div
               (let [images (get-in encounter-result-specs [encounter-result :images])]
                 (for [image images]
                   [:img {:src image}]))
               (if (= :battle encounter-result)
                 (battle-display warriors brigands)
                 (display-buttons [end-turn-button]))])])]]
       [:div
        [:text (str (:name (current-player)) "'s turn.")]])]]
   [:div {:id "player-data"}
    [:table
     [:tbody
      [:tr
       [:th]
       (doall
         (for [player (ordered-players)]
           ^{:key (str "th-" (:name player))}
           [:th (:name player)]))]
      (doall
        (for [data-key ordered-keys
              :let [players (ordered-players)]]
          (data-row-for data-key players)))]]]])

(defn name-input []
  [:div
   [:h4
    "Name, please:"
    [:input
     {:id "txt-playername"
      :type "text"
      :value (:player-name @model/game-state)
      :on-change #(model/update-player-name! (-> % .-target .-value))}]]])

(defn new-game-button []
  [:button
   {:id "btn-new-game"
    :class "button brown"
    :on-click #(communication/new-game)}
   "New Game"])

(defn start-game-button []
  [:button
   {:id "btn-start-game"
    :class "button yellow"
    :on-click #(communication/start-game)}
   "Start Game"])

(defn kingdom-marker [kingdom size loc r w]
  [:text (str " of " (name kingdom))]
  #_(let [stroke (second (color constants/colors))
        fill (first (color constants/colors))]
    [:svg
     {:height (to-scale size)
      :width (to-scale size)}
     [:circle
      {:cx (to-scale loc)
       :cy (to-scale loc)
       :r (to-scale r)
       :stroke stroke
       :stroke-width (to-scale w)
       :fill fill}]]))

(defn initializing-table-rows []
  [[:tr
    {:key "token-row"}
    [:td
     [:span
      "Token: "]
     [:span
      {:class "leading"}
      (get-in @model/game-state [:server-state :token])]]]
   [:tr
    {:key "players-row"}
    [:td
     "Current players:"
     [:br]
     [:ul
      (for [player (get-in @model/game-state [:server-state :players])]
        ^{:key player}
        [:li (:name player)
         [kingdom-marker (:kingdom player) 12 6 5 1]])]]]
   (when (not (:joining-game-token @model/game-state))
     [:tr
      {:key "start-button-row"}
      [:td
       {:style {:text-align "center"}}
       [start-game-button]]])])

(defn start-a-game []
  [:div
   {:style {:display "inline-block"
            :vertical-align "top"
            :margin "5px 5px 5px 5px"}}
   [:table
    [:thead
     [:tr
      [:th
       {:style {:text-align "center"}}
       "Starting a game"]]]
    [:tbody
     (let [server-state (:server-state @model/game-state)]
       (if (not server-state)
         [:tr
          [:td
           {:style {:text-align "center"}}
           [new-game-button]]]
         (for [row (initializing-table-rows)]
           ^{:key row}
           row)))]]])

(defn join-a-game []
  [:div
   {:style {:display "inline-block"
            :vertical-align "top"
            :margin "5px 5px 5px 5px"}}
   [:table
    [:tr
     [:th
      {:style {:text-align "center"}}
      "Joining a game"]]
    [:tr
     [:td
      {:style {:text-align "center"}}
      [:text "Enter game code:"]
      [:input
       {:id "txt-game-token"
        :type "text"
        :value (:joining-game-token @model/game-state)
        :on-change #(model/update-joining-game-token! (-> % .-target .-value))}]]]
    [:tr
     [:td
      {:style {:text-align "center"}}
      [:button
       {:id "btn-join-game"
        :class "button brown"
        :on-click #(communication/join-game)}
       "Join Game"]]]]])

(defn main []
  [:center
   [:div
    [:h2 "Dark Tower"]
    (if (get-in @model/game-state [:server-state :game-on?])
      [:div
       [game-area]
       (if (:game-over @model/game-state)
         [:div
          {:style {:display "inline-block"
                   :vertical-align "top"
                   :margin "5px 5px 5px 5px"}}
          [:h2 "WE HAVE A WINNER!"]
          [:h3 (str "Congratulations, " (:name (current-player)) "!")]]
         [player-area])]
      [:div
       [name-input]
       [start-a-game]
       (when (not (:server-state @model/game-state))
         [join-a-game])])

    [:hr]
    [:div
     [:span
      {:style {:font-family "Helvetica"
               :font-size "10px"}}
      "Client game state: " (with-out-str (pprint/pprint @model/game-state))]]]])
