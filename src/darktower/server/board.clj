(ns darktower.server.board)

(defn potential-neighbors-for [{:keys [row idx]}]
  (let [prev-row (- row 1)
        next-row (inc row)]
    [{:row prev-row :idx (- idx 1)}
     {:row prev-row :idx idx}
     {:row row :idx (- idx 1)}
     {:row row :idx (inc idx)}
     {:row next-row :idx idx}
     {:row next-row :idx (inc idx)}]))

(defn back-edge? [{:keys [row idx]}]
  (= idx (inc row)))

(defn territory-beyond-back-edge [row territory-info]
  (and
    (= (:row territory-info) row)
    (> (:idx territory-info) (inc row))))

(defn filter-for-back-edge [{:keys [row idx] :as territory-info} maybe-neighbors]
  (if (back-edge? territory-info)
    (->> maybe-neighbors
         (remove #(territory-beyond-back-edge (dec row) %)))
    maybe-neighbors))

(defn remove-beyond-front-edge [neighbors]
  (remove #(< (:idx %) 0) neighbors))

(defn remove-beyond-back-edge-in-row [row neighbors]
  (remove #(and
            (= (:row %) row)
            (> (:idx %) (inc row))) neighbors))

(defn remove-beyond-back-edge [row neighbors]
  (->> neighbors
       (remove-beyond-back-edge-in-row (dec row))
       (remove-beyond-back-edge-in-row row)))

(defn remove-above-top-row [neighbors]
  neighbors)

(defn remove-below-bottom-row [neighbors]
  neighbors)

(defn maybe-add-frontier [idx neighbors]
  (if (zero? idx)
    (conj neighbors :frontier)
    neighbors))

(defn neighbors-for [territory-info]
  (cond
    #_#_(= :dark-tower territory-info)
    (for [i (range 0 3)]
      {:row 1 :idx i})

    #_#_(= :frontier territory-info)
    (for [i (range 1 6)]
      {:row i :idx 0})

    (:row territory-info)
    (let [{:keys [row idx]} territory-info
          maybe-neighbors (potential-neighbors-for territory-info)]
      (->> maybe-neighbors
           (remove-beyond-front-edge)
           (remove-beyond-back-edge row)
           (remove-above-top-row)
           (remove-below-bottom-row)
           (maybe-add-frontier idx)

           ))))
