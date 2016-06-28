(ns darktower.views.board
  (:require [clojure.string :as str]
            [darktower.communication :as communication]
            [darktower.model :as model]))

(def territories-in-kingdom
  [{:id 0
    :type :dark-tower
    :neighbors [{:row 1 :idx 0} {:row 1 :idx 1} {:row 1 :idx 2}]}

   {:id 1
    :type :frontier
    :neighbors [{:row 1 :idx 0} {:row 2 :idx 0} {:row 3 :idx 0} {:row 4 :idx 0} {:row 5 :idx 0}]}

   {:row 1
    :idx 0
    :type :territory
    :neighbors [:dark-tower :frontier {:row 1 :idx 1} {:row 2 :idx 0} {:row 2 :idx 1}]}
   {:row 1
    :idx 1
    :type :territory
    :neighbors [:dark-tower {:row 1 :idx 0} {:row 1 :idx 2} {:row 2 :idx 1} {:row 2 :idx 2}]}
   {:row 1
    :idx 2
    :type :territory
    :neighbors [:dark-tower {:row 1 :idx 1} {:row 2 :idx 2} {:row 2 :idx 3}]}

   {:row 2
    :idx 0
    :type :territory
    :neighbors [:frontier {:row 1 :idx 0} {:row 2 :idx 1} {:row 3 :idx 0} {:row 3 :idx 1}]}
   {:row 2
    :idx 1
    :type :territory
    :neighbors [{:row 1 :idx 0} {:row 1 :idx 1} {:row 2 :idx 0} {:row 2 :idx 2} {:row 3 :idx 1} {:row 3 :idx 2}]}
   {:row 2
    :idx 2
    :type :ruin
    :neighbors [{:row 1 :idx 1} {:row 1 :idx 2} {:row 2 :idx 1} {:row 2 :idx 3} {:row 3 :idx 2} {:row 3 :idx 3}]}
   {:row 2
    :idx 3
    :type :territory
    :neighbors [{:row 1 :idx 2} {:row 2 :idx 2} {:row 3 :idx 3} {:row 3 :idx 4}]}

   {:row 3
    :idx 0
    :type :territory
    :neighbors [:frontier {:row 2 :idx 0} {:row 3 :idx 1} {:row 4 :idx 0} {:row 4 :idx 1}]}
   {:row 3
    :idx 1
    :type :territory
    :neighbors [{:row 2 :idx 0} {:row 2 :idx 1} {:row 3 :idx 0} {:row 3 :idx 2} {:row 4 :idx 1} {:row 4 :idx 2}]}
   {:row 3
    :idx 2
    :type :bazaar
    :neighbors [{:row 2 :idx 1} {:row 2 :idx 2} {:row 3 :idx 1} {:row 3 :idx 3} {:row 4 :idx 2} {:row 4 :idx 3}]}
   {:row 3
    :idx 3
    :type :territory
    :neighbors [{:row 2 :idx 2} {:row 2 :idx 3} {:row 3 :idx 2} {:row 3 :idx 4} {:row 4 :idx 3} {:row 4 :idx 4}]}
   {:row 3
    :idx 4
    :type :territory
    :neighbors [{:row 2 :idx 3} {:row 3 :idx 3} {:row 4 :idx 4} {:row 4 :idx 5}]}

   {:row 4
    :idx 0
    :type :territory
    :neighbors []}
   {:row 4
    :idx 1
    :type :tomb
    :neighbors []}
   {:row 4
    :idx 2
    :type :territory
    :neighbors []}
   {:row 4
    :idx 3
    :type :territory
    :neighbors []}
   {:row 4
    :idx 4
    :type :sanctuary
    :neighbors []}
   {:row 4
    :idx 1
    :type :territory
    :neighbors []}

   {:row 5
    :idx 0
    :type :territory
    :neighbors []}
   {:row 5
    :idx 1
    :type :territory
    :neighbors []}
   {:row 5
    :idx 2
    :type :territory
    :neighbors []}
   {:row 5
    :idx 3
    :type :citadel
    :neighbors []}
   {:row 5
    :idx 4
    :type :territory
    :neighbors []}
   {:row 5
    :idx 5
    :type :territory
    :neighbors []}
   {:row 5
    :idx 6
    :type :territory
    :neighbors []}

   ])

(def board-spec
  {:min-radius 100
   :cx 450
   :cy 350})

(def kingdom-specs
  [{:kingdom :arisilon
    :angle-offset 51
    :angle-width 78
    :stroke-color "darkred"
    :fill-color "salmon"}
   {:kingdom :brynthia
    :angle-offset 141
    :angle-width 78
    :stroke-color "deepskyblue"
    :fill-color "lightcyan"}
   {:kingdom :durnin
    :angle-offset 231
    :angle-width 78
    :stroke-color "saddlebrown"
    :fill-color "tan"}
   {:kingdom :zenon
    :angle-offset 321
    :angle-width 78
    :stroke-color "darkgreen"
    :fill-color "mediumseagreen"}])

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

(defn path-for [{:keys [cx cy]} top-arc-offset r angle-offset arc-angle territory-idx]
  (let [bottom-arc (arc-for cx cy r (+ angle-offset (* territory-idx arc-angle)) (+ angle-offset (* (inc territory-idx) arc-angle)))
        top-arc (arc-for cx cy (- r top-arc-offset) (+ angle-offset (* territory-idx arc-angle)) (+ angle-offset (* (inc territory-idx) arc-angle)))
        bottom-arc-path (arc bottom-arc)
        right-line (str/join " " ["L" (:end-x top-arc) (:end-y top-arc)])
        top-line (str/join " " ["L" (:move-x top-arc) (:move-y top-arc)])]
    (str/join " " [bottom-arc-path right-line top-line])))

(defn path [territory-path stroke stroke-width fill dest-info]
  ^{:key territory-path}
  [:path
   {:d territory-path
    :stroke stroke
    :stroke-width stroke-width
    :fill fill
    :on-click #(communication/territory-click dest-info)}])

(defn row-spec [row]
  {:r (+ (:min-radius board-spec) (* row 50))
   :territory-count (+ 2 row)})

(defn arc-angle-for [kingdom-spec territory-count]
  (/ (:angle-width kingdom-spec) territory-count))

(defn generate-territories [kingdom-spec]
  (for [row (range 5 0 -1)
          :let [{:keys [r territory-count]} (row-spec row)
                arc-angle (arc-angle-for kingdom-spec territory-count)]]
    (for [territory-idx (range 0 territory-count)
          :let [territory-path (path-for board-spec 50 r (:angle-offset kingdom-spec) arc-angle territory-idx)]]
      (path territory-path (:stroke-color kingdom-spec) 1 (:fill-color kingdom-spec) {:row row :idx territory-idx}))))

(defn generate-frontier [frontier-spec]
  (let [r (+ (:min-radius board-spec) 250)
        frontier-path (path-for board-spec 250 r (:angle-offset frontier-spec) (:angle-width frontier-spec) 1)]
    (path frontier-path (:stroke-color frontier-spec) 1 (:fill-color frontier-spec) :frontier)))

(defn generate-dark-tower [dark-tower-spec]
  (let [r (:min-radius board-spec)
        dark-tower-path (path-for board-spec 50 r (:angle-offset dark-tower-spec) (:angle-width dark-tower-spec) 1)]
    (path dark-tower-path (:stroke-color dark-tower-spec) 1 (:fill-color dark-tower-spec) :dark-tower)))

(defn piece-image [x y w h img]
  ^{:key (str x "-" y "-" w "-" h "-" img)}
  [:g
   {:dangerouslySetInnerHTML
    {:__html (str "<image xlink:href=\"" img "\" x=\"" x "\" y=\"" y "\" width=\"" w "\" height=\"" h "\" />")}}])

(defn territory-arc-for [kingdom row idx]
  (let [{:keys [r territory-count]} (row-spec row)
        kingdom-spec (first (filter #(= kingdom (:kingdom %)) kingdom-specs))
        arc-angle (arc-angle-for kingdom-spec territory-count)]
    (arc-for (:cx board-spec)
             (:cy board-spec)
             r
             (+ (:angle-offset kingdom-spec) (* idx arc-angle))
             (+ (:angle-offset kingdom-spec) (* (inc idx) arc-angle)))))

(defn player-images []
  (for [player (get-in @model/game-state [:server-state :players])
        :let [{:keys [kingdom row idx]} (:current-territory player)
              territory-arc (territory-arc-for kingdom row idx)]]
    (piece-image (+ 10 (:end-x territory-arc)) (+ 10 (:end-y territory-arc)) 30 30 "img/golden_knight.png")))

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
        dark-tower))
    [:rect
     {:x 415
      :y 315
      :width 70
      :height 70
      :stroke "black"
      :stroke-width 1
      :fill "dimgray"}]
    (piece-image 400 300 100 100 "img/dtflag.gif")
    (player-images)]])
