(ns darktower.views.board
  (:require [clojure.string :as str]))

;; Thank you, smart people!  http://jsbin.com/cibicecuto/edit?html,js,output
(defn polar-to-cartesian [cx cy r angle-in-degrees]
  (let [angle-in-radians (/ (* (.-PI js/Math) (- angle-in-degrees 90)) 180)]
    {:x (+ cx (* r (.cos js/Math angle-in-radians)))
     :y (+ cy (* r (.sin js/Math angle-in-radians)))}))

(defn arc-for [cx cy r start-angle end-angle]
  (let [start (polar-to-cartesian cx cy r end-angle)
        end (polar-to-cartesian cx cy r start-angle)
        arc-sweep (if (<= (- end-angle start-angle) 180)
                    "0"
                    "1")]
    {:move-x (:x start)
     :move-y (:y start)
     :arc-sweep arc-sweep
     :r r
     :end-x (:x end)
     :end-y (:y end)}))

(defn arc [{:keys [move-x move-y r arc-sweep end-x end-y]}]
  (str/join " " ["M" move-x move-y "A" r r 0 arc-sweep 0 end-x end-y]))

(def board-spec
  {:min-radius 100
   :cx 450
   :cy 350})

(def kingdom-specs
  [{:angle-offset 51
    :angle-width 78
    :stroke-color "darkred"
    :fill-color "salmon"}
   {:angle-offset 141
    :angle-width 78
    :stroke-color "darkgreen"
    :fill-color "mediumseagreen"}
   {:angle-offset 231
    :angle-width 78
    :stroke-color "saddlebrown"
    :fill-color "tan"}
   {:angle-offset 321
    :angle-width 78
    :stroke-color "deepskyblue"
    :fill-color "lightcyan"}])

(def frontier-specs
  [{:angle-offset 27
    :angle-width 12
    :stroke-color "gold"
    :fill-color "moccasin"}
   {:angle-offset 117
    :angle-width 12
    :stroke-color "gold"
    :fill-color "moccasin"}
   {:angle-offset 207
    :angle-width 12
    :stroke-color "gold"
    :fill-color "moccasin"}
   {:angle-offset 297
    :angle-width 12
    :stroke-color "gold"
    :fill-color "moccasin"}])

(defn path-for [{:keys [cx cy]} top-arc-offset r angle-offset arc-angle territory-idx]
  (let [bottom-arc (arc-for cx cy r (+ angle-offset (* territory-idx arc-angle)) (+ angle-offset (* (inc territory-idx) arc-angle)))
        top-arc (arc-for cx cy (- r top-arc-offset) (+ angle-offset (* territory-idx arc-angle)) (+ angle-offset (* (inc territory-idx) arc-angle)))
        bottom-arc-path (arc bottom-arc)
        right-line (str/join " " ["L" (:end-x top-arc) (:end-y top-arc)])
        top-line (str/join " " ["L" (:move-x top-arc) (:move-y top-arc)])]
    (str/join " " [bottom-arc-path right-line top-line])))

(defn path [territory-path stroke stroke-width fill]
  ^{:key territory-path}
  [:path
   {:d territory-path
    :stroke stroke
    :stroke-width stroke-width
    :fill fill}])

(defn generate-territories [kingdom-spec]
  (for [row (range 5 0 -1)
          :let [r (+ (:min-radius board-spec) (* row 50))
                territory-count (+ 2 row)]]
    (for [territory-idx (range 0 territory-count)
          :let [arc-angle (/ (:angle-width kingdom-spec) territory-count)
                territory-path (path-for board-spec 50 r (:angle-offset kingdom-spec) arc-angle territory-idx)]]
      (path territory-path (:stroke-color kingdom-spec) 1 (:fill-color kingdom-spec)))))

(defn generate-frontier [frontier-spec]
  (let [r (+ (:min-radius board-spec) 250)
        frontier-path (path-for board-spec 250 r (:angle-offset frontier-spec) (:angle-width frontier-spec) 1)]
    (path frontier-path (:stroke-color frontier-spec) 1 (:fill-color frontier-spec))))

(def dark-tower-specs
  [{:angle-offset 45
    :angle-width 90
    :stroke-color "black"
    :fill-color "dimgray"}
   {:angle-offset 135
    :angle-width 90
    :stroke-color "black"
    :fill-color "dimgray"}
   {:angle-offset 225
    :angle-width 90
    :stroke-color "black"
    :fill-color "dimgray"}
   {:angle-offset 315
    :angle-width 90
    :stroke-color "black"
    :fill-color "dimgray"}])

(defn generate-dark-tower [dark-tower-spec]
  (let [r (:min-radius board-spec)
        dark-tower-path (path-for board-spec 50 r (:angle-offset dark-tower-spec) (:angle-width dark-tower-spec) 1)]
    (path dark-tower-path (:stroke-color dark-tower-spec) 1 (:fill-color dark-tower-spec))))

(defn main []
  [:div
   {:style {:display "inline-block"
            :vertical-align "top"
            :margin "5px 5px 5px 5px"}}
   [:svg
      {:id "svg-box"
       :width 900
       :height 700
       :style {:border "0.5px solid black"
               :background-color "darkgray"}}
    (for [frontier-spec frontier-specs]
      (let [frontier (generate-frontier frontier-spec)]
        frontier))
    (for [kingdom-spec kingdom-specs]
      (for [territory (generate-territories kingdom-spec)]
        territory))
    (for [dark-tower-spec dark-tower-specs]
      (let [dark-tower (generate-dark-tower dark-tower-spec)]
        dark-tower))]])
