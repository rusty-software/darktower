(ns darktower.view
  (:require [cljs.pprint :as pprint]
            [darktower.model :as model]
            [darktower.communication :as communication]
            [darktower.views.board :as board]))

(def tiles
  {:xlf {:img "extra_large_frontier.png"
         :w 240
         :h 120
         :color "yellow"}
   :lf {:img "large_frontier.png"
        :w 180
        :h 120
        :color "yellow"}
   :f {:img "frontier.png"
       :w 120
       :h 120
       :color "yellow"}
   :sf {:img "small_frontier.png"
        :w 60
        :h 120
        :color "yellow"}
   :dtfl {:img "dark_tower_frontier_left.png"
          :w 120
          :h 120
          :color "black"}
   :dt {:img "dark_tower.png"
        :w 120
        :h 120
        :color "black"}
   :dtfr {:img "dark_tower_frontier_right.png"
          :w 120
          :h 120
          :color "black"}
   :lfl {:img "land_frontier_left.png"
         :w 120
         :h 120
         :color "green"}
   :l {:img "land.png"
       :w 120
       :h 120
       :color "green"}
   :lfr {:img "land_frontier_right.png"
         :w 120
         :h 120
         :color "green"}
   :r {:img "ruin.png"
       :w 120
       :h 120
       :color "darkgray"}
   :t {:img "tomb.png"
       :w 120
       :h 120
       :color "lightgray"}
   :b {:img "bazaar.png"
       :w 120
       :h 120
       :color "wheat"}
   :s {:img "sanctuary.png"
       :w 120
       :h 120
       :color "ivory"}
   :c {:img "citadel.png"
       :w 120
       :h 120
       :color "purple"}})

(def kingdom-layout
  [[:xlf :dtfl :dt :dtfr :xlf]
   [:lf :lfl :l :r :lfr :lf]
   [:f :lfl :l :b :l :lfr :f]
   [:sf :lfl :t :l :l :s :lfr :sf]
   [:lfl :l :l :c :l :l :lfr]])

(defn space [tile x y]
  ^{:key (str "tile-" tile "-" x "-" y)}
  [:rect
   {:x x
    :y y
    :width (:w tile)
    :height (:h tile)
    :stroke "black"
    :stroke-width 0.5
    :fill (:color tile)
    :fill-opacity 0.5}])

(defn spaces-for [territory-row y]
  (loop [x 0
         tile-keys territory-row
         spaces []]
    (if (not (seq tile-keys))
      spaces
      (let [tile-key (first tile-keys)
            tile (tile-key tiles)
            new-x (+ x (:w tile))
            space (space tile x y)]
        (recur new-x (rest tile-keys) (conj spaces space))))))

(defn territory-rows-for [layout]
  (loop [territory-rows layout
         y 0
         spaces []]
    (if (not (seq territory-rows))
      spaces
      (recur (rest territory-rows) (+ y (:h ((ffirst territory-rows) tiles))) (conj spaces (spaces-for (first territory-rows) y))))))

(defn game-area []
  (board/main)

  #_[:div
   {:style {:display "inline-block"
            :vertical-align "top"
            :margin "5px 5px 5px 5px"}}

   [:svg
      {:id "svg-box"
       :width 900
       :height 600
       :style {:border "0.5px solid black"}}
    (for [row (territory-rows-for kingdom-layout)]
      (for [space row]
        space))]
   ])

(defn current-player [])
(defn player-area [])

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

(defn kingdom-marker [color size loc r w]
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
         [kingdom-marker (:color player) 12 6 5 1]])]]]
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
    [:h1 "Dark Tower: the board game"]
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
      "Client game state: " (with-out-str (pprint/pprint @model/game-state))]]]])
