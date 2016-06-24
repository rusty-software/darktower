(ns darktower.views.board
  (:require [clojure.string :as str]))

;; NOTES
;; frontiers are going to be 12 degrees wide
;;

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
     :end-x (:x end)
     :end-y (:y end)}))

(defn describe-arc [cx cy r start-angle end-angle]
  (let [arc (arc-for cx cy r start-angle end-angle)]
    (clojure.string/join " " ["M" (:move-x arc) (:move-y arc) "A" r r 0 (:arc-sweep arc) 0 (:end-x arc) (:end-y arc)])))

(defn main []
  (println "polar-to-cartesian" (polar-to-cartesian 450 350 350 90))
  (println "describe-arc" (describe-arc 450 350 350 0 90))
  [:div
   {:style {:display "inline-block"
            :vertical-align "top"
            :margin "5px 5px 5px 5px"}}

   [:svg
      {:id "svg-box"
       :width 900
       :height 700
       :style {:border "0.5px solid black"}}
    [:circle
     {:cx 450
      :cy 350
      :r 350
      :stroke "black"
      :stroke-width 0.5
      :fill "lightgray"}]
    [:circle
      {:cx 450
       :cy 350
       :r 300
       :stroke "white"
       :stroke-width 0.5
       :fill "none"}]
    [:circle
      {:cx 450
       :cy 350
       :r 250
       :stroke "white"
       :stroke-width 0.5
       :fill "none"}]
    [:circle
      {:cx 450
       :cy 350
       :r 200
       :stroke "white"
       :stroke-width 0.5
       :fill "none"}]
    [:circle
      {:cx 450
       :cy 350
       :r 150
       :stroke "white"
       :stroke-width 0.5
       :fill "none"}]
    [:circle
      {:cx 450
       :cy 350
       :r 100
       :stroke "white"
       :stroke-width 0.5
       :fill "none"}]
    [:line
     {:x1 0
      :y1 0
      :x2 30
      :y2 30
      :stroke "purple"
      :stroke-width 1}]
    ;; FRONTIERS
    [:path
     {:d (describe-arc 450 350 350 39 51)
      :stroke "gold"
      :stroke-width "5"
      :fill "none"}]
    [:path
     {:d (describe-arc 450 350 350 129 141)
      :stroke "gold"
      :stroke-width "5"
      :fill "none"}]
    [:path
     {:d (describe-arc 450 350 350 219 231)
      :stroke "gold"
      :stroke-width "5"
      :fill "none"}]
    [:path
     {:d (describe-arc 450 350 350 309 321)
      :stroke "gold"
      :stroke-width "5"
      :fill "none"}]

    ;; SOUTHERN KINGDOM
    [:path
     {:d (clojure.string/join " " [(describe-arc 450 350 350 141 (+ 141 (/ 78 7)))])
      :stroke "darkgreen"
      :stroke-width "5"
      :fill "none"}]
    [:path
     {:d (describe-arc 450 350 350 (+ 141 (/ 78 7)) (+ 141 (* 2 (/ 78 7))))
      :stroke "lightgreen"
      :stroke-width "5"
      :fill "none"}]
    (let [bottom-arc (arc-for 450 350 350 (+ 141 (* 2 (/ 78 7))) (+ 141 (* 3 (/ 78 7))))
          concentric-arc (arc-for 450 350 300 (+ 141 (* 2 (/ 78 7))) (+ 141 (* 3 (/ 78 7))))
          right-line (str/join " " ["L" (:end-x concentric-arc) (:end-y concentric-arc)])
          move (str/join " " ["M" (:move-x bottom-arc) (:move-y bottom-arc)])
          left-line (str/join " " ["L" (:move-x concentric-arc) (:move-y concentric-arc)])]
      [:path
       {:d (str/join " " [(describe-arc 450 350 350 (+ 141 (* 2 (/ 78 7))) (+ 141 (* 3 (/ 78 7))))
                          right-line
                          move
                          left-line
                          ])
        :stroke "green"
        :stroke-width "5"
        :fill "none"}])
    [:path
     {:d (describe-arc 450 350 350 (+ 141 (* 3 (/ 78 7))) (+ 141 (* 4 (/ 78 7))))
      :stroke "limegreen"
      :stroke-width "5"
      :fill "none"}]
    [:path
     {:d (describe-arc 450 350 350 (+ 141 (* 4 (/ 78 7))) (+ 141 (* 5 (/ 78 7))))
      :stroke "forestgreen"
      :stroke-width "5"
      :fill "none"}]
    [:path
     {:d (describe-arc 450 350 350 (+ 141 (* 5 (/ 78 7))) (+ 141 (* 6 (/ 78 7))))
      :stroke "springgreen"
      :stroke-width "5"
      :fill "none"}]
    [:path
     {:d (describe-arc 450 350 350 (+ 141 (* 6 (/ 78 7))) 219)
      :stroke "seagreen"
      :stroke-width "5"
      :fill "none"}]

    [:path
     {:d (describe-arc 450 350 300 141 (+ 141 (/ 78 6)))
      :stroke "orangered"
      :stroke-width "5"
      :fill "none"}]
    [:path
     {:d (describe-arc 450 350 300 (+ 141 (/ 78 6)) (+ 141 (* 2 (/ 78 6))))
      :stroke "lightsalmon"
      :stroke-width "5"
      :fill "none"}]
    [:path
     {:d (describe-arc 450 350 300 (+ 141 (* 2 (/ 78 6))) (+ 141 (* 3 (/ 78 6))))
      :stroke "chocolate"
      :stroke-width "5"
      :fill "none"}]]
   ])
