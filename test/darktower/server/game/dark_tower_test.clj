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

(deftest current-lock-test
  (let [riddle-of-the-keys [:gold-key :brass-key :silver-key]]
    (testing "given a full set of remaining keys, current lock is first"
      (is (= :gold-key (current-lock riddle-of-the-keys [:brass-key :silver-key :gold-key]))))
    (testing "given a smaller set of remaining keys, current lock is second"
      (is (= :brass-key (current-lock riddle-of-the-keys [:brass-key :silver-key]))))))

(deftest try-key-test
  (let [riddle-of-the-keys [:gold-key :brass-key :silver-key]]
    (testing "given a correct guess on first key, advances to second"
      (let [remaining-keys [:brass-key :silver-key :gold-key]
            result (try-key riddle-of-the-keys remaining-keys :gold-key)]
        (is (= [:brass-key :silver-key] (:remaining-keys result)))
        (is (= :successful-try (:result result)))))
    (testing "given a correct guess on second key, advances to dark tower battle"
      (let [remaining-keys [:brass-key :silver-key]
            result (try-key riddle-of-the-keys remaining-keys :brass-key)]
        (is (= :dark-tower-battle (:result result)))))
    (testing "given an incorrect guess, bad guess is returned"
      (let [result (try-key riddle-of-the-keys [:brass-key :silver-key] :silver-key)]
        (is (= :wrong-key (:result result)))
        (is (= [:brass-key :silver-key] (:remaining-keys result)))))))
