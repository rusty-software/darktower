(ns darktower.server.game-test
  (:require [clojure.test :refer :all]
            [darktower.server.game :refer :all]))

(deftest initialize-player-test
  (testing "Player is initialized with proper warriors, gold, food, and location"
    (let [player {:uid nil
                  :name "rusty"
                  :kingdom :zenon}
          expected (merge player {:current-territory {:kingdom :zenon :row 5 :idx 3}
                                  :warriors 10
                                  :gold 30
                                  :food 25})]
      (is (= expected (initialize-player player))))))

(deftest initialize-game-test
  (let [players [{:uid "14" :name "brian" :kingdom :zenon}
                 {:uid "12" :name "michael" :kingdom :brynthia}
                 {:uid "15" :name "rusty" :kingdom :arisilon}]
        game-state (initialize-game players)]
    (testing "Player are listed appropriately"
      (let [init-players (:players game-state)
            player-order (:player-order game-state)]
        (is (= 3 (count init-players)))
        (is (= "14" (first player-order)))
        (is (= "12" (second player-order)))
        (is (= "15" (last player-order)))))
    (testing "Current player is first in players list"
      (is (= "14" (:current-player game-state))))))
