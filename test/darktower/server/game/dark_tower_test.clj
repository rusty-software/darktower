(ns darktower.server.game.dark-tower-test
  (:require [clojure.test :refer :all]
            [darktower.server.test-helpers :refer :all]
            [darktower.server.game.dark-tower :refer :all]))

(deftest next-key-test
  (testing "rotates through all available keys"
    (let [keys [:brass-key :silver-key :gold-key]]
      (is (= :silver-key (next-key keys :brass-key)))
      (is (= :gold-key (next-key keys :silver-key)))
      (is (= :brass-key (next-key keys :gold-key))))
    (let [keys [:brass-key :gold-key]]
      (is (= :gold-key (next-key keys :brass-key)))
      (is (= :brass-key (next-key keys :gold-key))))))

(deftest guess-key-test
  (testing "given a correct guess on first key, advances to second")
  (testing "given a correct guess on second key, advances to dark tower battle"))
