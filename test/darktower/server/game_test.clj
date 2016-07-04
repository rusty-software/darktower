(ns darktower.server.game-test
  (:require [clojure.test :refer :all]
            [schema.core :as s]
            [darktower.server.game :refer :all]
            [darktower.server.schema :as schema]))

(def player
  {:uid "15"
   :name "rusty"
   :kingdom :arisilon})

(deftest initialize-player-test
  (testing "Player is initialized with proper warriors, gold, food, and location"
    (let [expected (merge player {:current-territory {:kingdom :arisilon :row 5 :idx 3}
                                  :warriors 10
                                  :gold 30
                                  :food 25
                                  :scout false
                                  :healer false
                                  :beast false
                                  :brass-key false
                                  :silver-key false
                                  :gold-key false
                                  :pegasus false
                                  :sword false})
          actual (initialize-player player)]
      (is (= expected actual))
      (s/validate schema/Player actual))))

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

(deftest valid-move-test
  (testing "Movement allowed to adjacent territories"
    (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2})
          expected {:valid? true}
          actual (valid-move player {:kingdom :zenon :row 3 :idx 1})]
      (is (= expected actual)))
    (let [player (assoc player :current-territory {:kingdom :zenon :row 1 :idx 2})
          expected {:valid? true}
          actual (valid-move player {:kingdom :zenon :type :frontier})]
      (is (= expected actual))))
  (testing "Movement to non-adjacent territories"
    (testing "When the player has no pegasus, movement is not allowed"
      (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2})
            expected {:valid? false
                      :reason "Destination must be adjacent to your current territory!"}
            actual (valid-move player {:kingdom :zenon :row 3 :idx 0})]
        (is (= expected actual))))
    (testing "When the player has a pegasus, movement is allowed and the pegasus is expended"
      (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2}
                                 :pegasus true)
            expected {:valid? true :pegasus-required? true}
            actual (valid-move player {:kingdom :zenon :row 1 :idx 0})]
        (is (= expected actual)))))
  (testing "Movement to another kingdom is only allowed from previous kingdom's frontier, even if the player has a pegasus"
    (let [player (assoc player :current-territory {:kingdom :zenon :type :frontier}
                               :pegasus true)
          expected {:valid? true}
          actual (valid-move player {:kingdom :arisilon :row 3 :idx 0})]
      (is (= expected actual)))
    (let [player (assoc player :current-territory {:kingdom :zenon :row 1 :idx 2}
                               :pegasus true)
          expected {:valid? false
                    :reason "Destination must be adjacent to your current territory!"}
          actual (valid-move player {:kingdom :arisilon :row 3 :idx 0})]
      (is (= expected actual)))))

#_(deftest safe-move-test
  (testing "Moves player from one adjacent territory to another"
    (let [expected {:move-result :moved :current-territory {:kingdom :arisilon :row 3 :idx 0}}]
      (is (= expected (safe-move nil false {:kingdom :arisilon :row 3 :idx 0})))))
  (testing "Indicates that a pegasus was used"
    (let [expected {:move-result :moved :pegasus-required? true :current-territory {:kingdom :arisilon :row 3 :idx 0}}]
      (is (= expected (safe-move nil true {:kingdom :arisilon :row 3 :idx 0}))))))

(deftest lost-test
  (testing "Lost moves the player back to the last territory"
    (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2}
                               :last-territory {:kingdom :zenon :row 3 :idx 1})
          expected (assoc player :encounter-result :lost
                                 :current-territory {:kingdom :zenon :row 3 :idx 1}
                                 :last-territory {:kingdom :zenon :row 3 :idx 1})
          actual (lost player)]
      (is (= expected actual))))
  (testing "Lost with scout advances and grants the player another turn"
    (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2}
                               :last-territory {:kingdom :zenon :row 3 :idx 1}
                               :scout true)
          expected (assoc player :encounter-result :lost
                                 :current-territory {:kingdom :zenon :row 3 :idx 2}
                                 :last-territory {:kingdom :zenon :row 3 :idx 1}
                                 :scout true
                                 :extra-turn true)
          actual (lost player)]
      (is (= expected actual)))))

(deftest plague-test
  (testing "Plague reduces the player's warrior count by 2"
    (let [player (assoc player :warriors 10)
          expected (assoc player :encounter-result :plague :warriors 8)
          actual (plague player)]
      (is (= expected actual))))
  (testing "Warrior count cannot drop below 1"
    (let [player (assoc player :warriors 2)
          expected (assoc player :encounter-result :plague :warriors 1)
          actual (plague player)]
      (is (= expected actual))))
  (testing "Plague with healer increases warrior count by 2"
    (let [player (assoc player :warriors 10 :healer true)
          expected (assoc player :encounter-result :plague :warriors 12 :healer true)
          actual (plague player)]
      (is (= expected actual))))
  (testing "Warrior count cannot exceed 99"
    (let [player (assoc player :warriors 98 :healer true)
          expected (assoc player :encounter-result :plague :warriors 99 :healer true)
          actual (plague player)]
      (is (= expected actual)))))

(deftest dragon-attack-test
  (testing "Dragon takes 25% (rounded down) of a player's warriors and gold and adds them to its own collection"
    (let [player (assoc player :warriors 10 :gold 10)
          dragon-hoard {:warriors 10 :gold 10}
          expected {:player (assoc player :encounter-result :dragon-attack
                                          :warriors 8
                                          :gold 8)
                    :dragon-hoard {:warriors 12 :gold 12}}
          actual (dragon-attack player dragon-hoard)]
      (is (= expected actual))))
#_  (testing "Dragon always takes at least one where possible"
    (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2}
                               :warriors 2
                               :gold 2)
          dragon-hoard {:warriors 10 :gold 10}
          expected {:move-result :dragon
                    :current-territory {:kingdom :zenon :row 3 :idx 1}
                    :dragon-hoard {:warriors 11 :gold 11}
                    :warriors 1
                    :gold 1}
          actual (dragon-attack player dragon-hoard {:kingdom :zenon :row 3 :idx 1})]
      (is (= expected actual))))
#_  (testing "Warrior count cannot drop below 1"
    (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2}
                               :warriors 1
                               :gold 1)
          dragon-hoard {:warriors 10 :gold 10}
          expected {:move-result :dragon
                    :current-territory {:kingdom :zenon :row 3 :idx 1}
                    :dragon-hoard dragon-hoard
                    :warriors 1
                    :gold 1}
          actual (dragon-attack player dragon-hoard {:kingdom :zenon :row 3 :idx 1})]
      (is (= expected actual))))
#_  (testing "Dragon with sword adds dragon's collection to players, resetting its own"
    (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2}
                               :warriors 10
                               :gold 10
                               :sword true)
          dragon-hoard {:warriors 10 :gold 10}
          expected {:move-result :dragon-sword
                    :current-territory {:kingdom :zenon :row 3 :idx 1}
                    :dragon-hoard {:warriors 0 :gold 0}
                    :warriors 20
                    :gold 20}
          actual (dragon-attack player dragon-hoard {:kingdom :zenon :row 3 :idx 1})]
      (is (= expected actual))))
#_  (testing "Warrior and gold counts cannot exceed 99"
    (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2}
                               :warriors 98
                               :gold 98
                               :sword true)
          dragon-hoard {:warriors 10 :gold 10}
          expected {:move-result :dragon-sword
                    :current-territory {:kingdom :zenon :row 3 :idx 1}
                    :dragon-hoard {:warriors 0 :gold 0}
                    :warriors 99
                    :gold 99}
          actual (dragon-attack player dragon-hoard {:kingdom :zenon :row 3 :idx 1})]
      (is (= expected actual)))))
