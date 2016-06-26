(ns darktower.server.board-test
  (:require [clojure.test :refer :all]
            [darktower.server.board :refer :all]))

(deftest potential-neighbors-test
  (testing "All territories have 6 potential neighbors"
    (let [expected [{:row 2 :idx 1} {:row 2 :idx 2} {:row 3 :idx 1} {:row 3 :idx 3} {:row 4 :idx 2} {:row 4 :idx 3}]
          territory-location {:row 3 :idx 2}]
      (is (= expected (potential-neighbors-for territory-location))))))

(deftest neighbors-test
  (testing "Interior territories have six neighbors"
    (let [expected [{:row 3 :idx 3} {:row 3 :idx 4} {:row 4 :idx 3} {:row 4 :idx 5} {:row 5 :idx 4} {:row 5 :idx 5}]
          territory-location {:row 4 :idx 4}]
      (is (= expected (neighbors-for territory-location)))))
  (testing "Territories (excluding rows 1 and 5) at end border 4 others"
    (let [expected [{:row 1 :idx 2} {:row 2 :idx 2} {:row 3 :idx 3} {:row 3 :idx 4}]
          territory-location {:row 2 :idx 3}]
      (is (= expected (neighbors-for territory-location)))))
  (testing "Territories (excluding rows 1 and 5) at 0 border a frontier and 4 others"
    (let [expected [:frontier {:row 1 :idx 0} {:row 2 :idx 1} {:row 3 :idx 0} {:row 3 :idx 1}]
          territory-location {:row 2 :idx 0}]
      (is (= expected (neighbors-for territory-location)))))
  (testing "Territories in row 5 have nothing below them"
    (let [expected [{:row 4 :idx 2} {:row 4 :idx 3} {:row 5 :idx 2} {:row 5 :idx 4}]
          territory-location {:row 5 :idx 3}]
      (is (= expected (neighbors-for territory-location)))))
  (testing "Territories in row 1 have nothing above them and have a dark tower"
    (let [expected [:dark-tower {:row 1 :idx 0} {:row 1 :idx 2} {:row 2 :idx 1} {:row 2 :idx 2}]
          territory-location {:row 1 :idx 1}]
      (is (= expected (neighbors-for territory-location)))))
  (testing "Dark Tower is only bordered by row 1"
    (let [expected [{:row 1 :idx 0} {:row 1 :idx 1} {:row 1 :idx 2}]
          territory-location :dark-tower]
      (is (= expected (neighbors-for territory-location)))))
  (testing "Frontier is only bordered by index 0"
    (let [expected [{:row 1 :idx 0} {:row 2 :idx 0} {:row 3 :idx 0} {:row 4 :idx 0} {:row 5 :idx 0}]
          territory-location :frontier]
      (is (= expected (neighbors-for territory-location))))))
