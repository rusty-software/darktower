(ns darktower.server.game.bazaar-test
  (:require [clojure.test :refer :all]
            [darktower.server.test-helpers :refer :all]
            [darktower.server.game.bazaar :refer :all]
            [darktower.server.game.main :as main]))

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

(deftest haggle-test
  (let [bazaar {:current-item :warrior
                :warrior 8
                :food 1
                :beast 20
                :scout 19
                :healer 18}]
    (testing "given successful haggle, reduces price by 1"
      (with-redefs [main/roll-dn (constantly 2)]
        (is (= 7 (:warrior (haggle bazaar))))
        (is (= 19 (:beast (haggle (assoc bazaar :current-item :beast)))))
        (is (= 18 (:scout (haggle (assoc bazaar :current-item :scout)))))
        (is (= 17 (:healer (haggle (assoc bazaar :current-item :healer)))))))
    (testing "given failed haggle, closes bazaar"
      (with-redefs [main/roll-dn (constantly 1)]
        (is (:closed? (haggle bazaar)))))
    (testing "bazaar closes when trying to haggle below min cost"
      (with-redefs [main/roll-dn (constantly 2)]
        (is (:closed? (haggle (assoc bazaar :warrior 4))))))))
