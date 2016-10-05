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

(deftest has-key?-test
  (is (has-key? player {:kingdom :arisilon}))
  (is (not (has-key? player {:kingdom :brynthia})))
  (is (has-key? (assoc player :brass-key true) {:kingdom :brynthia}))
  (is (not (has-key? player {:kingdom :durnin})))
  (is (has-key? (assoc player :silver-key true) {:kingdom :durnin}))
  (is (not (has-key? player {:kingdom :zenon})))
  (is (has-key? (assoc player :gold-key true) {:kingdom :zenon})))

(defn top-row-edge [player kingdom]
  (assoc player :current-territory {:kingdom kingdom :row 1 :idx 2}))

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
  (with-redefs [roll-100 (constantly 50.0)]
    (testing "Given an even number of brigands and warrior win, brigands reduced by half"
      (let [player (assoc player :warriors 10 :brigands 10)
            expected {:player (assoc player :encounter-result :fighting-won-round
                                            :brigands 5)}]
        (is (= expected (fight player)))))
    (testing "Given an odd number of brigands and warrior win, brigands reduced by half rounded down"
      (let [player (assoc player :warriors 12 :brigands 11)
            expected {:player (assoc player :encounter-result :fighting-won-round
                                            :brigands 5)}]
        (is (= expected (fight player)))))
    (testing "Given a brigand win, warriors reduced by one"
      (let [player (assoc player :warriors 9 :brigands 10)
            expected {:player (assoc player :encounter-result :fighting-lost-round
                                            :warriors 8)}]
        (is (= expected (fight player)))))
    (testing "When warriors are reduced to 1, battle ends in loss"
      (let [player (assoc player :warriors 2 :brigands 10)
            expected {:player (assoc player :encounter-result :fighting-lost
                                            :warriors 1)}]
        (is (= expected (fight player)))))
    (testing "When the brigands are reduced to 1 and warriors win, battle ends in victory (with treasure)"
      (let [player (assoc (top-row-edge player :brynthia) :warriors 10 :brigands 1 :gold 10)
            {:keys [encounter-result warriors gold brass-key pegasus sword]} (:player (fight player))]
        (is (= 10 warriors))
        (is (= :fighting-won encounter-result))
        (is (or (< 10 gold)
              brass-key
              pegasus
              sword))))))

(deftest can-receive-treasure?-test
  (let [player (assoc (top-row-edge player :arisilon) :gold 10)]
    (is (can-receive-treasure? (assoc player :pegasus true :sword true)) "can receive gold")
    (is (can-receive-treasure? (assoc player :gold 99 :sword true)) "can receive sword")
    (is (can-receive-treasure? (assoc player :gold 99 :pegasus true)) "can receive pegasus")
    (is (not (can-receive-treasure? (assoc player :gold 99 :pegasus true :sword true))) "cannot receive a key in home kingdom")
    (is (can-receive-treasure? (assoc (top-row-edge player :brynthia) :gold 99 :pegasus true :sword true)) "can receive key")
    (is (not (can-receive-treasure? (assoc (top-row-edge player :brynthia) :gold 99 :brass-key true :pegasus true :sword true))))
    (is (can-receive-treasure? (assoc (top-row-edge player :durnin) :gold 99 :brass-key true :pegasus true :sword true)) "can receive key")
    (is (not (can-receive-treasure? (assoc (top-row-edge player :durnin) :gold 99 :brass-key true :silver-key true :pegasus true :sword true))))
    (is (can-receive-treasure? (assoc (top-row-edge player :zenon) :gold 99 :brass-key true :silver-key true :pegasus true :sword true)) "can receive key")
    (is (not (can-receive-treasure? (assoc (top-row-edge player :zenon) :gold 99 :brass-key true :silver-key true :gold-key true :pegasus true :sword true))))
    (is (not (can-receive-treasure? (assoc player :gold 99 :brass-key true :silver-key true :gold-key true :pegasus true :sword true))))
    (is (can-receive-treasure? (assoc player :gold 99 :brass-key true :silver-key true :gold-key true :pegasus true :sword true) true))
    (is (not (can-receive-treasure? (assoc player :gold 99 :brass-key true :silver-key true :gold-key true :pegasus true :sword true :wizard true) true)))))

(deftest treasure-type-test
  (with-redefs [roll-100 (constantly 30)]
    (is (= :gold (treasure-type (roll-100) true))))
  (with-redefs [roll-100 (constantly 50)]
    (is (= :key (treasure-type (roll-100) true))))
  (with-redefs [roll-100 (constantly 70)]
    (is (= :pegasus (treasure-type (roll-100) true))))
  (with-redefs [roll-100 (constantly 85)]
    (is (= :sword (treasure-type (roll-100) true))))
  (with-redefs [roll-100 (constantly 100)]
    (is (= :wizard (treasure-type (roll-100) true))))
  (with-redefs [roll-100 (constantly 100)]
    (is (= :gold (treasure-type (roll-100) nil)))))

(deftest tried-everything?-test
  (is (not (tried-everything? #{} true)))
  (is (not (tried-everything? #{:gold} true)))
  (is (not (tried-everything? #{:gold :key} true)))
  (is (not (tried-everything? #{:gold :key :pegasus} true)))
  (is (not (tried-everything? #{:gold :key :pegasus :sword} true)))
  (is (tried-everything? #{:gold :key :pegasus :sword :wizard} true))
  (is (tried-everything? #{:gold :key :pegasus :sword} nil)))

(deftest can-award-key?-test
  (is (can-award-key? (assoc player :current-territory {:kingdom :brynthia})))
  (is (not (can-award-key? (assoc player :current-territory {:kingdom :brynthia} :brass-key true))))
  (is (can-award-key? (assoc player :current-territory {:kingdom :durnin})))
  (is (not (can-award-key? (assoc player :current-territory {:kingdom :durnin} :silver-key true))))
  (is (can-award-key? (assoc player :current-territory {:kingdom :zenon})))
  (is (not (can-award-key? (assoc player :current-territory {:kingdom :zenon} :gold-key true))))
  (is (not (can-award-key? (assoc player :current-territory {:kingdom :arisilon}))))
  (is (not (can-award-key? (assoc player :current-territory {:kingdom :arisilon} :brass-key true :silver-key true :gold-key true))))
  )

(deftest can-award?-test
  (let [player (assoc (top-row-edge player :arisilon) :gold 10)]
    (is (can-award? :gold player))
    (is (not (can-award? :gold (assoc player :gold 99))))
    (is (not (can-award? :key (assoc player :current-territory {:kingdom :arisilon}))))
    (is (can-award? :key (assoc player :current-territory {:kingdom :brynthia})))
    (is (not (can-award? :key (assoc player :current-territory {:kingdom :brynthia} :brass-key true))))
    (is (can-award? :key (assoc player :current-territory {:kingdom :durnin} :brass-key true)))
    (is (not (can-award? :key (assoc player :current-territory {:kingdom :durnin} :brass-key true :silver-key true))))
    (is (can-award? :key (assoc player :current-territory {:kingdom :zenon} :brass-key true :silver-key true)))
    (is (can-award? :pegasus player))
    (is (not (can-award? :pegasus (assoc player :pegasus true))))
    (is (can-award? :sword player))
    (is (not (can-award? :sword (assoc player :sword true))))
    (is (can-award? :wizard player true))
    (is (not (can-award? :wizard (assoc player :wizard true) true)))
    (is (not (can-award? :wizard player)))))

(deftest award-treasure-test
  (let [player (assoc (top-row-edge player :arisilon) :gold 10)]
    (is (< 10 (:gold (award-treasure :gold player))))
    (is (:brass-key (award-treasure :key (assoc player :current-territory {:kingdom :brynthia}))))
    (is (:silver-key (award-treasure :key (assoc player :current-territory {:kingdom :durnin}))))
    (is (:gold-key (award-treasure :key (assoc player :current-territory {:kingdom :zenon}))))
    (is (:pegasus (award-treasure :pegasus player)))
    (is (:sword (award-treasure :sword player)))
    (is (:wizard (award-treasure :wizard player)))))

(deftest treasure-test
  (testing "Given a roll 30 or below, increases gold"
    (with-redefs [roll-100 (constantly 30)]
      (testing "increases gold"
        (let [player (assoc player :gold 10)]
          (is (< 10 (:gold (treasure player))))))
      (testing "does not award more than max"
        (let [player (assoc player :gold 90)]
          (is (= 99 (:gold (treasure player))))))))
  (testing "Given a roll between 31 and 50, gives a key"
    (let [player (assoc (top-row-edge player :arisilon) :gold 10)]
      (with-redefs [roll-100 (constantly 50)]
        (testing "Key type depends on relative country"
          (is (not (:brass-key player)))
          (is (:brass-key (treasure (assoc player :current-territory {:kingdom :brynthia}))))
          (is (not (:silver-key player)))
          (is (:silver-key (treasure (assoc player :current-territory {:kingdom :durnin} :brass-key true))))
          (is (not (:gold-key player)))
          (is (:gold-key (treasure (assoc player :current-territory {:kingdom :zenon} :brass-key true :silver-key true))))))))
  (testing "Given a roll between 51 and 70, gives a pegasus"
    (let [player (assoc (top-row-edge player :arisilon) :gold 10)]
      (with-redefs [roll-100 (constantly 70)]
        (is (not (:pegasus player)))
        (is (:pegasus (treasure player))))))
  (testing "Given a roll between 71 and 85, gives a sword"
    (let [player (assoc (top-row-edge player :arisilon) :gold 10)]
      (with-redefs [roll-100 (constantly 85)]
        (is (not (:sword player)))
        (is (:sword (treasure player))))))
  (testing "Given a roll between 86 and 100 in a multi-player game, gives a wizard"
    (let [player (assoc (top-row-edge player :arisilon) :gold 10)]
      (with-redefs [roll-100 (constantly 100)]
        (is (not (:wizard player)))
        (is (:wizard (treasure player true))))))
  (testing "Given a player with everything, awards gold"
    (let [times-called (atom 0)]
      (with-redefs [roll-100 (fn []
                               (swap! times-called inc)
                               (case @times-called
                                 1 50
                                 2 70
                                 3 85
                                 4 100))]
        (let [player (assoc (top-row-edge player :arisilon) :gold 10 :brass-key true :silver-key true :gold-key true :pegasus true :sword true)]
          (is (< 10 (:gold (treasure player))))))))
  (testing "Given a player with everything and max gold, awards nothing"
    (let [player (assoc (top-row-edge player :arisilon) :gold 99 :brass-key true :silver-key true :gold-key true :pegasus true :sword true)]
      (is (= player (treasure player))))))
