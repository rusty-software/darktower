(ns darktower.server.game-test
  (:require [clojure.test :refer :all]
            [schema.core :as s]
            [darktower.server.test-helpers :refer :all]
            [darktower.server.game :refer :all]
            [darktower.server.schema :as schema]
            [darktower.server.game.main :refer [roll-d100 roll-dn]]))

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

(deftest requires-key?-test
  (is (not (requires-key? player {:kingdom :arisilon :row 1 :idx 2})))
  (is (not (requires-key? player {:kingdom :arisilon :type :frontier})))
  (is (not (requires-key? player {:kingdom :brynthia :row 1 :idx 2})))
  (is (requires-key? player {:kingdom :brynthia :type :frontier}))
  (is (not (requires-key? player {:kingdom :durnin :row 1 :idx 2})))
  (is (requires-key? player {:kingdom :durnin :type :frontier}))
  (is (not (requires-key? player {:kingdom :zenon :row 1 :idx 2})))
  (is (requires-key? player {:kingdom :zenon :type :frontier})))

(deftest valid-move-test
  (testing "Movement allowed to adjacent territories"
    (let [player (assoc player :current-territory {:kingdom :arisilon :row 3 :idx 2})]
      (is (= {:valid? true} (valid-move player {:kingdom :arisilon :row 3 :idx 1})))
      (is (= {:valid? true} (valid-move (top-row-edge player :arisilon) {:kingdom :arisilon :type :frontier})))))
  (testing "Movement to non-adjacent territories"
    (testing "When the player has no pegasus, movement is not allowed"
      (is (= {:valid? false
              :message "Destination must be adjacent to your current territory!"}
            (valid-move (top-row-edge player :arisilon) {:kingdom :arisilon :row 3 :idx 0}))))
    (testing "When the player has a pegasus, movement is allowed and the pegasus is expended"
      (is (= {:valid? true :pegasus-required? true}
            (valid-move (assoc (top-row-edge player :arisilon) :pegasus true) {:kingdom :arisilon :row 1 :idx 0})))))
  (testing "Movement to another kingdom is only allowed from previous kingdom's frontier, even if the player has a pegasus"
    (let [player (assoc player :current-territory {:kingdom :zenon :type :frontier}
                               :pegasus true)]
      (is (= {:valid? true} (valid-move player {:kingdom :arisilon :row 3 :idx 0}))))
    (let [player (assoc (top-row-edge player :zenon) :pegasus true)]
      (is (= {:valid? false
              :message "Destination must be adjacent to your current territory!"}
            (valid-move player {:kingdom :arisilon :row 3 :idx 0})))))
  (testing "Movement to a frontier requires the appropriate key"
    (is (= {:valid? true} (valid-move (top-row-edge player :arisilon) {:kingdom :arisilon :type :frontier})))
    (is (= {:valid? true} (valid-move (assoc player :current-territory {:kingdom :arisilon :type :frontier}) {:kingdom :brynthia :row 3 :idx 0})))
    (is (= {:valid? false :message "Key missing!"} (valid-move (top-row-edge player :brynthia) {:kingdom :brynthia :type :frontier})))
    (is (= {:valid? true} (valid-move (assoc (top-row-edge player :brynthia) :brass-key true) {:kingdom :brynthia :type :frontier})))
    (is (= {:valid? false :message "Key missing!"} (valid-move (top-row-edge player :durnin) {:kingdom :durnin :type :frontier})))
    (is (= {:valid? true} (valid-move (assoc (top-row-edge player :durnin) :silver-key true) {:kingdom :durnin :type :frontier})))
    (is (= {:valid? false :message "Key missing!"} (valid-move (top-row-edge player :zenon) {:kingdom :zenon :type :frontier})))
    (is (= {:valid? true} (valid-move (assoc (top-row-edge player :zenon) :gold-key true) {:kingdom :zenon :type :frontier})))))

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
    (let [player (assoc player :warriors 10 :gold 25)
          expected {:player (assoc player :encounter-result :plague :warriors 8 :gold 25)}
          actual (plague player)]
      (is (= expected actual))))
  (testing "Plague reduces the player's gold as necessary"
    (let [player (assoc player :warriors 10 :gold 60)
          expected {:player (assoc player :encounter-result :plague :warriors 8 :gold 48)}
          actual (plague player)]
      (is (= expected actual))))
  (testing "Warrior count cannot drop below 1"
    (let [player (assoc player :warriors 2 :gold 12)
          expected {:player (assoc player :encounter-result :plague :warriors 1 :gold 6)}
          actual (plague player)]
      (is (= expected actual))))
  (testing "Plague with healer increases warrior count by 2"
    (let [player (assoc player :warriors 10 :gold 25 :healer true)
          expected {:player (assoc player :encounter-result :plague-healer :warriors 12 :gold 25 :healer true)}
          actual (plague player)]
      (is (= expected actual))))
  (testing "Warrior count cannot exceed 99"
    (let [player (assoc player :warriors 98 :gold 25 :healer true)
          expected {:player (assoc player :encounter-result :plague-healer :warriors 99 :gold 25 :healer true)}
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

(deftest flee-test
  (testing "Fleeing reduces the warrior count by 1"
    (is (= 1 (get-in (flee {:warriors 2}) [:player :warriors]))))
  (testing "Fleeing cannot reduce warrior count below 1"
    (is (= 1 (get-in (flee {:warriors 1}) [:player :warriors])))))

(deftest winning-chance-test
  (is (= 50.0 (winning-chance 10 10)))
  (is (= 62.5 (winning-chance 10 5)))
  (is (= 70.0 (winning-chance 10 2)))
  (is (= 72.5 (winning-chance 10 1)))
  (is (= 47.5 (winning-chance 9 10)))
  (is (= 37.5 (winning-chance 5 10))))

(deftest fight-test
  (with-redefs [roll-d100 (constantly 50.0)]
    (testing "Given an even number of brigands and warrior win, brigands reduced by half"
      (let [player (assoc player :warriors 10 :brigands 10 :gold 10)
            expected {:player (assoc player :encounter-result :fighting-won-round
                                            :brigands 5)}]
        (is (= expected (fight player)))))
    (testing "Given an odd number of brigands and warrior win, brigands reduced by half rounded down"
      (let [player (assoc player :warriors 12 :brigands 11 :gold 10)
            expected {:player (assoc player :encounter-result :fighting-won-round
                                            :brigands 5)}]
        (is (= expected (fight player)))))
    (testing "Given a brigand win, warriors reduced by one with commensurate gold loss"
      (let [player (assoc player :warriors 9 :brigands 10 :gold 54)
            expected {:player (assoc player :encounter-result :fighting-lost-round
                                            :warriors 8
                                            :gold 48)}]
        (is (= expected (fight player)))))
    (testing "When warriors are reduced to 1, battle ends in loss"
      (let [player (assoc player :warriors 2 :brigands 10 :gold 6)
            expected {:player (assoc player :encounter-result :fighting-lost
                                            :warriors 1)}]
        (is (= expected (fight player)))))
    (testing "When the brigands are reduced to 1 and warriors win, battle ends in victory (with treasure)"
      (let [player (assoc (top-row-edge player :brynthia) :warriors 10 :brigands 1 :gold 10)
            {:keys [encounter-result warriors brigands gold brass-key pegasus sword]} (:player (fight player))]
        (is (= 10 warriors))
        (is (nil? brigands))
        (is (= :fighting-won encounter-result))
        (is (or (< 10 gold)
              brass-key
              pegasus
              sword))))))

(deftest encounter-location-ruin-test
  (let [params {:type :tomb :player (assoc (initialize-player player) :current-territory {:kingdom :brynthia :row 5 :idx 3})}]
    (testing "Given a roll <= 20, nothing happens"
      (with-redefs [roll-d100 (constantly 20)]
        (let [result (encounter-location params)]
          (is (= :safe-move (get-in result [:player :encounter-result]))))))
    (testing "Given a roll between 20 and 30, treasure is awarded"
      (with-redefs [roll-d100 (constantly 30)]
        (let [result (encounter-location params)]
          (is (= :safe-move (get-in result [:player :encounter-result])))
          (is (or (< 30 (get-in result [:player :gold]))
                (get-in result [:player :brass-key])
                (get-in result [:player :pegasus])
                (get-in result [:player :sword]))))))
    (testing "Given a roll between 30 and 100, a battle ensues"
      (with-redefs [roll-d100 (constantly 100)]
        (let [result (encounter-location params)]
          (is (= :battle (get-in result [:player :encounter-result]))))))))

(deftest encounter-location-tomb-test
  (let [params {:type :tomb :player (assoc (initialize-player player) :current-territory {:kingdom :brynthia :row 5 :idx 3})}]
    (testing "Given a roll <= 20, nothing happens"
      (with-redefs [roll-d100 (constantly 20)]
        (let [result (encounter-location params)]
          (is (= :safe-move (get-in result [:player :encounter-result]))))))
    (testing "Given a roll between 20 and 30, treasure is awarded"
      (with-redefs [roll-d100 (constantly 30)]
        (let [result (encounter-location params)]
          (is (= :safe-move (get-in result [:player :encounter-result])))
          (is (or (< 30 (get-in result [:player :gold]))
                (get-in result [:player :brass-key])
                (get-in result [:player :pegasus])
                (get-in result [:player :sword]))))))
    (testing "Given a roll between 30 and 100, a battle ensues"
      (with-redefs [roll-d100 (constantly 100)]
        (let [result (encounter-location params)]
          (is (= :battle (get-in result [:player :encounter-result]))))))))

(deftest encounter-location-sanctuary-test
  (testing "Given 4 or fewer warriors, award warriors"
    (with-redefs [roll-dn (constantly 3)]
      (let [gets-new-warriors (assoc player :warriors 4 :food 10 :gold 10)
            no-new-warriors (assoc gets-new-warriors :warriors 5)]
        (is (= 12 (:warriors (encounter-location {:type :sanctuary :player gets-new-warriors}))))
        (is (= 5 (:warriors (encounter-location {:type :sanctuary :player no-new-warriors})))))))
  (testing "Given 7 or less gold, award gold"
    (with-redefs [roll-dn (constantly 6)]
      (let [gets-new-gold (assoc player :warriors 10 :food 10 :gold 7)
            no-new-gold (assoc gets-new-gold :gold 8)]
        (is (= 23 (:gold (encounter-location {:type :sanctuary :player gets-new-gold}))))
        (is (= 8 (:gold (encounter-location {:type :sanctuary :player no-new-gold})))))))
  (testing "Given 5 or less food, award food"
    (with-redefs [roll-dn (constantly 6)]
      (let [gets-new-food (assoc player :warriors 10 :food 5 :gold 10)
            no-new-food (assoc gets-new-food :food 6)]
        (is (= 21 (:food (encounter-location {:type :sanctuary :player gets-new-food}))))
        (is (= 6 (:food (encounter-location {:type :sanctuary :player no-new-food})))))))
  (testing "Given all keys possessed, home kingdom, between 5 and 24 warriors, and not having visited a santuary or citadel last, doubles warriors")
  )
