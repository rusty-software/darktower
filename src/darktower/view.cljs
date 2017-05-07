(ns darktower.view
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.pprint :as pprint]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [darktower.model :as model]
            [darktower.communication :as communication]
            [darktower.views.displays :as displays]
            [darktower.views.board :as board]))

;; TODO: clear display on "your turn"
;; TODO: picking the wrong key on dark tower should end your turn

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

(defn player-area []
  [:div
   {:style
    {:display "inline-block"
     :width "490px"}}
   [:div {:id "darktower"}
    [:div {:id "darktower-display"}
     (let [cp (current-player)
           encounter-result (:encounter-result cp)]
       (cond
         (and (my-turn?) (game-over? encounter-result))
         [displays/current-player-victory-display (first (get-in encounter-result-specs [:dark-tower-won :images])) cp]

         (game-over? encounter-result)
         [displays/other-player-victory-display cp]

         (my-turn?)
         [displays/current-player-display encounter-result-specs cp]

         :default
         [displays/other-player-display (:name cp)]))]]
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
