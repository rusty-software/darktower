(ns darktower.view)

(defn space [x y]
  [:rect
   {:x x
    :y y
    :width 80
    :height 80
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
     :width 500
     :height 300
     :style {:border "0.5px solid black"}}
    [space 0 0]]

   ])

(defn main []
  (game-area))
