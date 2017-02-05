(ns darktower.server.game.bazaar-test
  (:require [clojure.test :refer :all]
            [darktower.server.test-helpers :refer :all]
            [darktower.server.game.bazaar :refer :all]
            [darktower.server.game.main :as main]))

(deftest init-bazaar-test
  (testing "initializes with basics and things the player is missing"
    (let [{:keys [warriors food beast scout healer]} (init player)]
      (is (every? #(not (nil? %)) [warriors food beast scout healer])))
    (let [{:keys [warriors food beast scout healer]} (init (assoc player :beast true))]
      (is (every? #(not (nil? %)) [warriors food scout healer]))
      (is (nil? beast)))
    (let [{:keys [warriors food beast scout healer]} (init (assoc player :beast true :scout true))]
      (is (every? #(not (nil? %)) [warriors food healer]))
      (is (nil? beast))
      (is (nil? scout)))
    (let [{:keys [warriors food beast scout healer]} (init (assoc player :beast true :scout true :healer true))]
      (is (every? #(not (nil? %)) [warriors food]))
      (is (nil? beast))
      (is (nil? scout))
      (is (nil? healer)))))

(deftest next-item-test
  (testing "rotates through all the available options"
    (let [bazaar (init player)]
      (is (= :warriors (:current-item bazaar)))
      (is (= :food (:current-item (next-item bazaar))))
      (is (= :beast (:current-item (next-item (assoc bazaar :current-item :food)))))
      (is (= :scout (:current-item (next-item (assoc bazaar :current-item :beast)))))
      (is (= :healer (:current-item (next-item (assoc bazaar :current-item :scout)))))
      (is (= :warriors (:current-item (next-item (assoc bazaar :current-item :healer))))))))

(deftest haggled-too-far?-test
  (testing "given a new price below the minimum, haggled too far"
    (is (#'darktower.server.game.bazaar/haggled-too-far? :warriors 3)))
  (testing "given a new price at the minimum, haggle is ok"
    (is (not (#'darktower.server.game.bazaar/haggled-too-far? :warriors 4)))))

(deftest haggle-test
  (let [bazaar {:current-item :warriors
                :warriors 8
                :food 1
                :beast 20
                :scout 19
                :healer 18}]
    (testing "given successful haggle, reduces price by 1"
      (with-redefs [main/roll-dn (constantly 2)]
        (is (= 7 (:warriors (haggle bazaar))))
        (is (= 19 (:beast (haggle (assoc bazaar :current-item :beast)))))
        (is (= 18 (:scout (haggle (assoc bazaar :current-item :scout)))))
        (is (= 17 (:healer (haggle (assoc bazaar :current-item :healer)))))))
    (testing "given failed haggle, closes bazaar"
      (with-redefs [main/roll-dn (constantly 1)]
        (is (:closed? (haggle bazaar)))))
    (testing "bazaar closes when trying to haggle below min cost"
      (with-redefs [main/roll-dn (constantly 2)]
        (is (:closed? (haggle (assoc bazaar :warriors 4))))))))
