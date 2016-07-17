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
                      :message "Destination must be adjacent to your current territory!"}
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
                    :message "Destination must be adjacent to your current territory!"}
          actual (valid-move player {:kingdom :arisilon :row 3 :idx 0})]
      (is (= expected actual)))))

(deftest safe-move-test
  (testing "Passes the player through"
    (is (= {:player (assoc player :encounter-result :safe-move)} (safe-move player)))))

(deftest lost-test
  (testing "Lost moves the player back to the last territory"
    (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2}
                               :last-territory {:kingdom :zenon :row 3 :idx 1})
          expected {:player (assoc player :encounter-result :lost
                                          :current-territory {:kingdom :zenon :row 3 :idx 1}
                                          :last-territory {:kingdom :zenon :row 3 :idx 1})}
          actual (lost player)]
      (is (= expected actual))))
  (testing "Lost with scout advances and grants the player another turn"
    (let [player (assoc player :current-territory {:kingdom :zenon :row 3 :idx 2}
                                         :last-territory {:kingdom :zenon :row 3 :idx 1}
                                         :scout true)
          expected {:player (assoc player :encounter-result :lost-scout
                                          :current-territory {:kingdom :zenon :row 3 :idx 2}
                                          :last-territory {:kingdom :zenon :row 3 :idx 1}
                                          :scout true
                                          :extra-turn true)}
          actual (lost player)]
      (is (= expected actual)))))

(deftest plague-test
  (testing "Plague reduces the player's warrior count by 2"
    (let [player (assoc player :warriors 10)
          expected {:player (assoc player :encounter-result :plague :warriors 8)}
          actual (plague player)]
      (is (= expected actual))))
  (testing "Warrior count cannot drop below 1"
    (let [player (assoc player :warriors 2)
          expected {:player (assoc player :encounter-result :plague :warriors 1)}
          actual (plague player)]
      (is (= expected actual))))
  (testing "Plague with healer increases warrior count by 2"
    (let [player (assoc player :warriors 10 :healer true)
          expected {:player (assoc player :encounter-result :plague-healer :warriors 12 :healer true)}
          actual (plague player)]
      (is (= expected actual))))
  (testing "Warrior count cannot exceed 99"
    (let [player (assoc player :warriors 98 :healer true)
          expected {:player (assoc player :encounter-result :plague-healer :warriors 99 :healer true)}
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
  (testing "Dragon always takes at least one where possible"
    (let [player (assoc player :warriors 2 :gold 2)
          dragon-hoard {:warriors 10 :gold 10}
          expected {:player (assoc player :encounter-result :dragon-attack
                                          :warriors 1
                                          :gold 1)
                    :dragon-hoard {:warriors 11 :gold 11}}
          actual (dragon-attack player dragon-hoard)]
      (is (= expected actual))))
  (testing "Warrior count cannot drop below 1"
    (let [player (assoc player :warriors 1 :gold 1)
          dragon-hoard {:warriors 10 :gold 10}
          expected {:player (assoc player :encounter-result :dragon-attack
                                          :warriors 1
                                          :gold 1)
                    :dragon-hoard {:warriors 10 :gold 10}}
          actual (dragon-attack player dragon-hoard)]
      (is (= expected actual))))
  (testing "Dragon with sword adds dragon's collection to players, resetting its own"
   (let [player (assoc player :warriors 10 :gold 10 :sword true)
          dragon-hoard {:warriors 10 :gold 10}
          expected {:player (assoc player :encounter-result :dragon-attack-sword
                                          :warriors 20
                                          :gold 20
                                          :sword false)
                    :dragon-hoard {:warriors 0 :gold 0}}
          actual (dragon-attack player dragon-hoard)]
      (is (= expected actual))))
  (testing "Warrior and gold counts cannot exceed 99"
    (let [player (assoc player :warriors 98 :gold 98 :sword true)
          dragon-hoard {:warriors 10 :gold 10}
          expected {:player (assoc player :encounter-result :dragon-attack-sword
                                          :warriors 99
                                          :gold 99
                                          :sword false)
                    :dragon-hoard {:warriors 0 :gold 0}}
          actual (dragon-attack player dragon-hoard)]
      (is (= expected actual)))))

(deftest feed-test
  (testing "Food level reduced by appropriate amounts"
    (is (zero? (:food (feed {:warriors 1 :food 1}))))
    (is (zero? (:food (feed {:warriors 15 :food 1}))))
    (is (zero? (:food (feed {:warriors 30 :food 2}))))
    (is (zero? (:food (feed {:warriors 45 :food 3}))))
    (is (zero? (:food (feed {:warriors 60 :food 4}))))
    (is (zero? (:food (feed {:warriors 75 :food 5}))))
    (is (zero? (:food (feed {:warriors 90 :food 6}))))
    (is (zero? (:food (feed {:warriors 99 :food 7})))))
  (testing "Food level cannot drop below 0"
    (is (zero? (:food (feed {:warriors 1 :food 0}))))))
