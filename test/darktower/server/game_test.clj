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

(def player
  {:uid "15"
   :name "rusty"
   :kingdom :arisilon})

(deftest safe-move-test
  (testing "Movement allowed to adjacent territories"
    (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2})
          expected {:move-result :moved :current-territory {:kingdom :zenon :row 3 :idx 1}}
          actual (safe-move player {:kingdom :zenon :row 3 :idx 1})]
      (is (= expected actual)))
    (let [player (assoc player :current-territory {:kingdom :zenon :row 1 :idx 2})
          expected {:move-result :moved :current-territory {:kingdom :zenon :type :frontier}}
          actual (safe-move player {:kingdom :zenon :type :frontier})]
      (is (= expected actual))))
  (testing "Movement to non-adjacent territories"
    (testing "When the player has no pegasus, movement is not allowed"
      (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2})
            expected {:move-result :invalid
                      :reason "Destination must be adjacent to your current territory!"
                      :current-territory {:kingdom :zenon :row 3 :idx 2}}
            actual (safe-move player {:kingdom :zenon :row 3 :idx 0})]
        (is (= expected actual))))
    (testing "When the player has a pegasus, movement is allowed and the pegasus is expended"
      (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2}
                                 :pegasus true)
            expected {:move-result :moved-pegasus :current-territory {:kingdom :zenon :row 1 :idx 0}}
            actual (safe-move player {:kingdom :zenon :row 1 :idx 0})]
        (is (= expected actual)))))
  (testing "Movement to another kingdom is only allowed from previous kingdom's frontier, even if the player has a pegasus"
    (let [player (assoc player :current-territory {:kingdom :zenon :type :frontier}
                               :pegasus true)
          expected {:move-result :moved :current-territory {:kingdom :arisilon :row 3 :idx 0}}
          actual (safe-move player {:kingdom :arisilon :row 3 :idx 0})]
      (is (= expected actual)))
    (let [player (assoc player :current-territory {:kingdom :zenon :row 1 :idx 2}
                               :pegasus true)
          expected {:move-result :invalid
                    :reason "Destination must be adjacent to your current territory!"
                    :current-territory {:kingdom :zenon :row 1 :idx 2}}
          actual (safe-move player {:kingdom :arisilon :row 3 :idx 0})]
      (is (= expected actual)))))

(deftest lost-test
  (testing "Lost leaves the player on the original territory"
    (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2})
          expected {:move-result :lost :current-territory {:kingdom :zenon :row 3 :idx 2}}
          actual (lost player {:kingdom :zenon :row 3 :idx 1})]
      (is (= expected actual))))
  (testing "Lost with scout advances and grants the player another turn"
    (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2}
                               :scout true)
          expected {:move-result :lost-scout :extra-turn true :current-territory {:kingdom :zenon :row 3 :idx 1}}
          actual (lost player {:kingdom :zenon :row 3 :idx 1})]
      (is (= expected actual)))))

(deftest plague-test
  (testing "Plague reduces the player's warrior count by 2"
    (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2}
                               :warriors 10)
          expected {:move-result :plague
                    :current-territory {:kingdom :zenon :row 3 :idx 1}
                    :warriors 8}
          actual (plague player {:kingdom :zenon :row 3 :idx 1})]
      (is (= expected actual))))
  (testing "Warrior count cannot drop below 1"
    (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2}
                               :warriors 2)
          expected {:move-result :plague
                    :current-territory {:kingdom :zenon :row 3 :idx 1}
                    :warriors 1}
          actual (plague player {:kingdom :zenon :row 3 :idx 1})]
      (is (= expected actual))))
  (testing "Plague with healer increases warrior count by 2"
    (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2}
                               :warriors 10
                               :healer true)
          expected {:move-result :plague-healer
                    :current-territory {:kingdom :zenon :row 3 :idx 1}
                    :warriors 12}
          actual (plague player {:kingdom :zenon :row 3 :idx 1})]
      (is (= expected actual))))
  (testing "Warrior count cannot exceed 99"
    (testing "Plague with healer increases warrior count by 2"
    (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2}
                               :warriors 98
                               :healer true)
          expected {:move-result :plague-healer
                    :current-territory {:kingdom :zenon :row 3 :idx 1}
                    :warriors 99}
          actual (plague player {:kingdom :zenon :row 3 :idx 1})]
      (is (= expected actual))))))

(deftest dragon-test
  (testing "Dragon takes 25% (rounded down) of a player's warriors and gold and adds them to its own collection"
    (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2}
                               :warriors 10
                               :gold 10)
          dragon-hoard {:warriors 10 :gold 10}
          expected {:move-result :dragon
                    :current-territory {:kingdom :zenon :row 3 :idx 1}
                    :dragon-hoard {:warriors 12 :gold 12}
                    :warriors 8
                    :gold 8}
          actual (dragon-attack player dragon-hoard {:kingdom :zenon :row 3 :idx 1})]
      (is (= expected actual))))
  (testing "Dragon always takes at least one where possible"
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
  (testing "Warrior count cannot drop below 1"
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
  (testing "Dragon with sword adds dragon's collection to players, resetting its own"
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
  (testing "Warrior and gold counts cannot exceed 99"
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

(deftest roll-action-test
  (testing "50 or lower is a safe-move"
    (is (= 'safe-move (roll-action 1)))
    (is (= 'safe-move (roll-action 50))))
  (testing "51 to 70 is battle"
    (is (= 'battle (roll-action 51)))
    (is (= 'battle (roll-action 70))))
  (testing "71 to 80 is lost"
    (is (= 'lost (roll-action 71)))
    (is (= 'lost (roll-action 80))))
  (testing "81 to 90 is plague"
    (is (= 'plague (roll-action 81)))
    (is (= 'plague (roll-action 90))))
  (testing "91 to 100 is dragon attack"
    (is (= 'dragon-attack (roll-action 91)))
    (is (= 'dragon-attack (roll-action 100)))))
