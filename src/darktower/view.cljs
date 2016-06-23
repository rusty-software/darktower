(ns darktower.view)

(def tiles
  {:xlf {:img "extra_large_frontier.png"
         :w 240
         :h 120}
   :lf {:img "large_frontier.png"
        :w 180
        :h 120}
   :f {:img "frontier.png"
       :w 120
       :h 120}
   :sf {:img "small_frontier.png"
        :w 60
        :h 120}
   :dtfl {:img "dark_tower_frontier_left.png"
          :w 120
          :h 120}
   :dt {:img "dark_tower.png"
        :w 120
        :h 120}
   :dtfr {:img "dark_tower_frontier_right.png"
          :w 120
          :h 120}
   :lfl {:img "land_frontier_left.png"
         :w 120
         :h 120}
   :l {:img "land.png"
       :w 120
       :h 120}
   :lfr {:img "land_frontier_right.png"
         :w 120
         :h 120}
   :r {:img "ruin.png"
       :w 120
       :h 120}
   :t {:img "tomb.png"
       :w 120
       :h 120}
   :b {:img "bazaar.png"
       :w 120
       :h 120}
   :s {:img "sanctuary.png"
       :w 120
       :h 120}
   :c {:img "citadel.png"
       :w 120
       :h 120}})

(def territory-map
  [[:xlf :dtfl :dt :dtfr :xlf]
   [:lf :lfl :l :r :lfr :lf]
   [:f :lfl :l :b :l :lfr :f]
   [:sf :lfl :t :l :l :s :lfr :sf]
   [:lfl :l :l :c :l :l :lfr]])

(defn space [x y]
  [:rect
   {:x x
    :y y
    :width 120
    :height 120
    :stroke "black"
    :stroke-width 0.5
    :fill "lightgray"
    :fill-opacity 0.5}])

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
    [space 0 0]]

   ])

(defn main []
  (game-area))
