(ns darktower.server.board)

(def kingdoms [:arisilon :brynthia :durnin :zenon])

(defn next-kingdom [kingdom]
  (let [i (.indexOf kingdoms kingdom)]
    (if (= i (dec (count kingdoms)))
      (first kingdoms)
      (get kingdoms (inc i)))))

(defn potential-neighbors-for [{:keys [kingdom row idx]}]
  (let [prev-row (dec row)
        next-row (inc row)]
    [{:kingdom kingdom :row prev-row :idx (dec idx)}
     {:kingdom kingdom :row prev-row :idx idx}
     {:kingdom kingdom :row row :idx (dec idx)}
     {:kingdom kingdom :row row :idx (inc idx)}
     {:kingdom kingdom :row next-row :idx idx}
     {:kingdom kingdom :row next-row :idx (inc idx)}]))

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

(defn maybe-add-frontier [kingdom row idx neighbors]
  (if (= idx (inc row))
    (conj neighbors {:kingdom kingdom :type :frontier})
    neighbors))

(defn maybe-add-dark-tower [kingdom row neighbors]
  (if (= 1 row)
    (conj neighbors {:kingdom kingdom :type :dark-tower})
    neighbors))

(defn neighbors-for [territory-info]
  (cond
    (= :dark-tower (:type territory-info))
    (for [i (range 0 3)]
      {:kingdom (:kingdom territory-info) :row 1 :idx i})

    (= :frontier (:type territory-info))
    (for [i (range 1 6)]
      {:kingdom (next-kingdom (:kingdom territory-info)) :row i :idx 0})

    (:row territory-info)
    (let [{:keys [row idx]} territory-info
          maybe-neighbors (potential-neighbors-for territory-info)]
      (->> maybe-neighbors
           (remove-beyond-front-edge)
           (remove-beyond-back-edge row)
           (remove-above-top-row)
           (remove-below-bottom-row)
           (maybe-add-frontier (:kingdom territory-info) row idx)
           (maybe-add-dark-tower (:kingdom territory-info) row)))))

(def territory-types
  {{:row 2 :idx 1} :ruin
   {:row 3 :idx 2} :bazaar
   {:row 4 :idx 1} :sanctuary
   {:row 4 :idx 4} :tomb
   {:row 5 :idx 3} :citadel})

(defn type-for [territory-info]
  (if (:row territory-info)
    (get territory-types territory-info :territory)
    (:type territory-info)))

(defn territory-for [territory-info]
  (assoc {:neighbors (neighbors-for territory-info)} :type (type-for territory-info)))
