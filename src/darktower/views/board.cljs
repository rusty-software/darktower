(ns darktower.views.board
  (:require [clojure.string :as str]
            [darktower.communication :as communication]
            [darktower.model :as model]))

(def board-spec
  {:min-radius 100
   :cx 450
   :cy 350})

(def kingdom-specs
  [{:kingdom :arisilon
    :angle-offset 51
    :angle-width 78
    :stroke-color "darkred"
    :fill-color "salmon"
    :player-img-x-offset -40
    :player-img-y-offset -20
    :player-img "img/golden_knight.png"}
   {:kingdom :brynthia
    :angle-offset 141
    :angle-width 78
    :stroke-color "deepskyblue"
    :fill-color "lightcyan"
    :player-img-x-offset -20
    :player-img-y-offset -40
    :player-img "img/golden_knight.png"}
   {:kingdom :durnin
    :angle-offset 231
    :angle-width 78
    :stroke-color "saddlebrown"
    :fill-color "tan"
    :player-img-x-offset 0
    :player-img-y-offset -20
    :player-img "img/golden_knight.png"}
   {:kingdom :zenon
    :angle-offset 321
    :angle-width 78
    :stroke-color "darkgreen"
    :fill-color "mediumseagreen"
    :player-img-x-offset -16
    :player-img-y-offset 0
    :player-img "img/golden_knight.png"}])

(def frontier-specs
  [{:kingdom :zenon
    :angle-offset 27
    :angle-width 12
    :stroke-color "gold"
    :fill-color "moccasin"}
   {:kingdom :arisilon
    :angle-offset 117
    :angle-width 12
    :stroke-color "gold"
    :fill-color "moccasin"}
   {:kingdom :brynthia
    :angle-offset 207
    :angle-width 12
    :stroke-color "gold"
    :fill-color "moccasin"}
   {:kingdom :durnin
    :angle-offset 297
    :angle-width 12
    :stroke-color "gold"
    :fill-color "moccasin"}])

(def dark-tower-specs
  [{:kingdom :zenon
    :angle-offset 45
    :angle-width 90
    :stroke-color "black"
    :fill-color "dimgray"}
   {:kingdom :arisilon
    :angle-offset 135
    :angle-width 90
    :stroke-color "black"
    :fill-color "dimgray"}
   {:kingdom :brynthia
    :angle-offset 225
    :angle-width 90
    :stroke-color "black"
    :fill-color "dimgray"}
   {:kingdom :durnin
    :angle-offset 315
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

(defn path [kingdom territory-path stroke stroke-width fill dest-info]
  ^{:key territory-path}
  [:path
   {:d territory-path
    :stroke stroke
    :stroke-width stroke-width
    :fill fill
    :on-click #(communication/territory-click (assoc dest-info :kingdom kingdom))}])

(defn row-spec [row]
  {:r (+ (:min-radius board-spec) (* row 50))
   :territory-count (+ 2 row)})

(defn arc-angle-for [kingdom-spec territory-count]
  (/ (:angle-width kingdom-spec) territory-count))

(defn generate-territories [{:keys [kingdom angle-offset stroke-color fill-color] :as kingdom-spec}]
  (for [row (range 5 0 -1)
          :let [{:keys [r territory-count]} (row-spec row)
                arc-angle (arc-angle-for kingdom-spec territory-count)]]
    (for [territory-idx (range 0 territory-count)
          :let [territory-path (path-for board-spec 50 r angle-offset arc-angle territory-idx)]]
      (path kingdom territory-path stroke-color 1 fill-color {:row row :idx territory-idx}))))

(defn generate-frontier [{:keys [kingdom angle-offset angle-width stroke-color fill-color]}]
  (let [r (+ (:min-radius board-spec) 250)
        frontier-path (path-for board-spec 250 r angle-offset angle-width 1)]
    (path kingdom frontier-path stroke-color 1 fill-color :frontier)))

(defn generate-dark-tower [{:keys [kingdom angle-offset angle-width stroke-color fill-color]}]
  (let [r (:min-radius board-spec)
        dark-tower-path (path-for board-spec 50 r angle-offset angle-width 1)]
    (path kingdom dark-tower-path stroke-color 1 fill-color :dark-tower)))

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
              {:keys [player-img player-img-x-offset player-img-y-offset]} (first (filter #(= kingdom (:kingdom %)) kingdom-specs))
              {:keys [move-x move-y end-x end-y]} (territory-arc-for kingdom row idx)
              x (+ (/ (+ move-x end-x) 2) player-img-x-offset)
              y (+ (/ (+ move-y end-y) 2) player-img-y-offset)
              ]]
    (piece-image x y 32 32 player-img)))

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
