(ns darktower.server.game.bazaar-test
  (:require [clojure.test :refer :all]
            [darktower.server.test-helpers :refer :all]
            [darktower.server.game.bazaar :refer :all]))

(deftest init-bazaar-test
  (testing "initializes with basics and things the player is missing"
    (let [{:keys [warrior food beast scout healer]} (init player)]
      (is (every? #(not (nil? %)) [warrior food beast scout healer])))
    (let [{:keys [warrior food beast scout healer]} (init (assoc player :beast true))]
      (is (every? #(not (nil? %)) [warrior food scout healer]))
      (is (nil? beast)))
    (let [{:keys [warrior food beast scout healer]} (init (assoc player :beast true :scout true))]
      (is (every? #(not (nil? %)) [warrior food healer]))
      (is (nil? beast))
      (is (nil? scout)))
    (let [{:keys [warrior food beast scout healer]} (init (assoc player :beast true :scout true :healer true))]
      (is (every? #(not (nil? %)) [warrior food]))
      (is (nil? beast))
      (is (nil? scout))
      (is (nil? healer)))))

(deftest next-item-test
  (testing "rotates through all the available options"
    (let [bazaar (init player)]
      (is (= :warrior (:current-item bazaar)))
      (is (= :food (:current-item (next-item bazaar))))
      (is (= :beast (:current-item (next-item (assoc bazaar :current-item :food)))))
      (is (= :scout (:current-item (next-item (assoc bazaar :current-item :beast)))))
      (is (= :healer (:current-item (next-item (assoc bazaar :current-item :scout)))))
      (is (= :warrior (:current-item (next-item (assoc bazaar :current-item :healer))))))))
