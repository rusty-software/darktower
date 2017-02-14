(ns darktower.server.game.dark-tower-test
  (:require [clojure.test :refer :all]
            [darktower.server.test-helpers :refer :all]
            [darktower.server.game.dark-tower :refer :all]))

(deftest next-key-test
  (testing "rotates through all available keys"))

(deftest guess-key-test
  (testing "given a correct guess on first key, advances to second")
  (testing "given a correct guess on second key, advances to dark tower battle"))
