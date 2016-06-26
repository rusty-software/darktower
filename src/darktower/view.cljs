(ns darktower.view)

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
  [:div
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

(defn main []
  [:center
   [:div
    [:h1 "Dark Tower"]
    [:h2 "The Board Game"]]]
  (game-area))
