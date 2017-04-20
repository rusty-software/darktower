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

(defn my-turn? []
  (= (:uid @model/game-state) (:uid (current-player))))

(defn game-over? [encounter-result]
  (= :dark-tower-won encounter-result))

(def ordered-keys [:warriors :gold :food :scout :healer :beast :brass-key :silver-key :gold-key :pegasus :sword])

(defn data-row-for [data-key players]
  ^{:key (str "tr-" data-key players)}
  [:tr
   [:td (name data-key)]
   (doall
     (for [player players
           :let [val (get player data-key)]]
       ^{:key (str "td-" player data-key)}
       [:td (when val (str val))]))])

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
   :battle {:images ["img/warriors.jpg" "img/brigands.jpg"]}
   :fighting-won-round {:images ["img/warriors.jpg" "img/brigands.jpg"]}
   :fighting-lost-round {:images ["img/warriors.jpg" "img/brigands.jpg"]}
   #{:fighting-won :gold} {:images ["img/gold.jpg"]}
   #{:fighting-won :pegasus} {:images ["img/pegasus.jpg"]}
   #{:fighting-won :sword} {:images ["img/sword.jpg"]}
   #{:fighting-won :brass-key} {:images ["img/brasskey.jpg"]}
   #{:fighting-won :silver-key} {:images ["img/silverkey.jpg"]}
   #{:fighting-won :gold-key} {:images ["img/goldkey.jpg"]}
   :fighting-lost {:images ["img/warriors.jpg"]}
   :fled {:images ["img/warriors.jpg"]}
   #{:sanctuary #{:warriors}} {:images ["img/warriors.jpg"]}
   #{:sanctuary #{:food}} {:images ["img/food.jpg"]}
   #{:sanctuary #{:gold}} {:images ["img/gold.jpg"]}
   #{:sanctuary #{:warriors :food}} {:images ["img/warriors.jpg" "img/food.jpg"]}
   #{:sanctuary #{:warriors :gold}} {:images ["img/warriors.jpg" "img/gold.jpg"]}
   #{:sanctuary #{:warriors :food :gold}} {:images ["img/warriors.jpg" "img/food.jpg" "img/gold.jpg"]}
   #{:sanctuary #{:food :gold}} {:images ["img/food.jpg" "img/gold.jpg"]}
   :bazaar-closed {:images ["img/bazaar.jpg"]}
   :warriors {:images ["img/warrior.jpg"]}
   :food {:images ["img/food.jpg"]}
   :beast {:images ["img/beast.jpg"]}
   :scout {:images ["img/scout.jpg"]}
   :healer {:images ["img/healer.jpg"]}
   :key-missing {:images ["img/keymissing.jpg"]}
   :brass-key {:images ["img/brasskey.jpg"]}
   :silver-key {:images ["img/silverkey.jpg"]}
   :gold-key {:images ["img/goldkey.jpg"]}
   :dark-tower-won-round {:images ["img/warriors.jpg" "img/brigands.jpg"]}
   :dark-tower-lost-round {:images ["img/warriors.jpg" "img/brigands.jpg"]}
   :dark-tower-fled {:images ["img/warriors.jpg"]}
   :dark-tower-won {:images ["img/victory.jpg"]}})

(defn display-buttons [buttons]
  [:div (for [button buttons]
          ^{:key (str "btn-" button)}
          [button])])

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

(defn battle-display [warriors brigands]
  [:div
   [:span (str warriors " warriors; " brigands " brigands")]
   (display-buttons [fight-button flee-button])])

(defn bazaar-display [bazaar-inventory insufficient-funds?]
  (if (not (:closed? bazaar-inventory))
    [:div
     [:span (str "Cost: " (get bazaar-inventory (:current-item bazaar-inventory)))]
     (if insufficient-funds?
       (display-buttons [next-button haggle-button end-turn-button])
       (display-buttons [next-button buy-button haggle-button end-turn-button]))]
    [:div
     (display-buttons [end-turn-button])]))

(defn dark-tower-display [dark-tower-status]
  [:div
   [:span "Does this key fit the lock?"]
   (display-buttons [no-button yes-button end-turn-button])])

(defn message-display [message]
  [:div
   [:span message]
   (display-buttons [end-turn-button])])

(defn current-player-display []
  [:div
   [:text "Your turn."]
   [:br]
   [:div
    {:class "dt-display"}
    (let [{:keys [encounter-result awarded bazaar-inventory dark-tower-status insufficient-funds? warriors brigands message]} (current-player)]
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
            (#{:battle :fighting-won-round :fighting-lost-round :dark-tower-won-round :dark-tower-lost-round} encounter-result)
            (battle-display warriors brigands)

            (= :bazaar encounter-result)
            (bazaar-display bazaar-inventory insufficient-funds?)

            (= :dark-tower encounter-result)
            (dark-tower-display dark-tower-status)

            message
            (message-display message)

            :else
            (display-buttons [end-turn-button]))])])]])

(defn other-player-display []
  [:div
   [:text (str (:name (current-player)) "'s turn.")]])

(defn player-area []
  [:div
   {:style
    {:display "inline-block"
     :width "490px"}}
   [:div {:id "darktower"}
    [:div {:id "darktower-display"}
     (let [encounter-result (:encounter-result (current-player))]
       (cond
         (and (my-turn?) (game-over? encounter-result))
         (println "game over on my turn")

         (game-over? encounter-result)
         (println "game over on someone else's turn")

         (my-turn?)
         [current-player-display]

         :default
         [other-player-display]))]]
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

(defn difficulty-input []
  [:div
   "Difficulty:"
   [:select {:on-change #(model/update-difficulty! (.. % -target -value))}
    [:option {:value 1} "17 - 32 Brigands"]
    [:option {:value 2} "33 - 64 Brigands"]
    [:option {:value 3} "17 - 64 Brigands"]]])

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
         (doall
           [:tr
            [:td
             {:style {:text-align "center"}}
             [difficulty-input]
             [new-game-button]]])
         (for [row (initializing-table-rows)]
           ^{:key row}
           row)))]]])

(defn join-a-game []
  [:div
   {:style {:display "inline-block"
            :vertical-align "top"
            :margin "5px 5px 5px 5px"}}
   [:table
    [:thead
     [:tr
      [:th
       {:style {:text-align "center"}}
       "Joining a game"]]]
    [:tbody
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
        "Join Game"]]]]]])

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
