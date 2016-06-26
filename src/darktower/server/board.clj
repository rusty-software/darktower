(ns darktower.server.board)

(defn potential-neighbors-for [{:keys [row idx]}]
  (let [prev-row (dec row)
        next-row (inc row)]
    [{:row prev-row :idx (dec idx)}
     {:row prev-row :idx idx}
     {:row row :idx (dec idx)}
     {:row row :idx (inc idx)}
     {:row next-row :idx idx}
     {:row next-row :idx (inc idx)}]))

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
  (remove #(> (:row %) 5) neighbors))

(defn remove-below-bottom-row [neighbors]
  (remove #(< (:row %) 1) neighbors))

(defn maybe-add-frontier [idx neighbors]
  (if (zero? idx)
    (conj neighbors :frontier)
    neighbors))

(defn maybe-add-dark-tower [row neighbors]
  (if (= 1 row)
    (conj neighbors :dark-tower)
    neighbors))

(defn neighbors-for [territory-info]
  (cond
    (= :dark-tower territory-info)
    (for [i (range 0 3)]
      {:row 1 :idx i})

    (= :frontier territory-info)
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
           (maybe-add-dark-tower row)))))

(def territory-types
  {{:row 2 :idx 1} :ruin
   {:row 3 :idx 2} :bazaar
   {:row 4 :idx 1} :sanctuary
   {:row 4 :idx 4} :tomb
   {:row 5 :idx 3} :citadel})

(defn type-for [territory-info]
  (if (:row territory-info)
    (get territory-types territory-info :territory)
    territory-info))

(defn territory-for [territory-info]
  (assoc {:neighbors (neighbors-for territory-info)} :type (type-for territory-info)))
