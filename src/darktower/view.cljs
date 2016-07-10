(ns darktower.view
  (:require [cljs.pprint :as pprint]
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
  [:tr
   [:td (name data-key)]
   (for [player players]
     [:td (get player data-key)])])

(defn ordered-players []
  (for [player-id (get-in @model/game-state [:server-state :player-order])]
    (player player-id)))

(def encounter-result-specs
  {:lost {:img "img/lost.jpg"}
   :plague {:img "img/plague.jpg"}
   :dragon-attack {:img "img/dragon.jpg"}
   :safe-move {:img "img/victory.jpg"}})

(defn player-area []
  [:div
   {:style
    {:display "inline-block"
     :width "490px"}}
   [:div {:id "darktower"}
    [:div {:id "darktower-display"}
     (when (my-turn)
       [:div
        [:img {:src (get-in encounter-result-specs [(:encounter-result (current-player)) :img])}]
        [:br]
        [:button "Yes"]
        [:button "No"]])]]
   [:div {:id "player-data"}
    [:table
     [:tr
      [:th]
      (for [player (ordered-players)]
        (do
          [:th (:name player)]))]
     (for [data-key ordered-keys
           :let [players (ordered-players)]]
       (data-row-for data-key players))]]])

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
