(ns darktower.server.game.treasure-test
  (:require [clojure.test :refer :all]
            [darktower.server.game.main :refer [roll-100]]
            [darktower.server.test-helpers :refer :all]
            [darktower.server.game.treasure :refer :all]))

(deftest has-key?-test
  (is (has-key? player {:kingdom :arisilon :territory-type :frontier}))
  (is (not (has-key? player {:kingdom :brynthia :territory-type :frontier})))
  (is (has-key? (assoc player :brass-key true) {:kingdom :brynthia :territory-type :frontier}))
  (is (not (has-key? (assoc player :brass-key true) {:kingdom :durnin :territory-type :frontier})))
  (is (has-key? (assoc player :brass-key true :silver-key true) {:kingdom :durnin :territory-type :frontier}))
  (is (not (has-key? (assoc player :brass-key true :silver-key true) {:kingdom :zenon :territory-type :frontier})))
  (is (has-key? (assoc player :brass-key true :silver-key true :gold-key true) {:kingdom :zenon :territory-type :frontier})))

(deftest adjust-gold-test
  (testing "Gold amount is limited by the number of warriors"
    (is (= 60 (adjust-gold 10 60 nil)))
    (is (= 60 (adjust-gold 20 60 false)))
    (is (= 48 (adjust-gold 8 60 nil))))
  (testing "Gold amount is limited by the number of warriors and beast"
    (is (= 62 (adjust-gold 2 62 true)))
    (is (= 56 (adjust-gold 1 60 true)))
    (is (= 99 (adjust-gold 10 110 true)))))

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
  (let [player (assoc (top-row-edge player :arisilon) :gold 10 :warriors 10)]
    (is (can-award? :gold player))
    (is (not (can-award? :gold (assoc player :gold 61))))
    (is (can-award? :gold (assoc player :gold 61 :warriors 50)))
    (is (not (can-award? :gold (assoc player :gold 99 :warriors 50))))
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
  (let [player (assoc (top-row-edge player :arisilon) :warriors 10 :gold 10)]
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
        (let [player (assoc player :warriors 10 :gold 10)]
          (is (< 10 (:gold (treasure player))))))
      (testing "does not award more than max"
        (let [player (assoc player :warriors 10 :beast true :gold 90)]
          (is (= 99 (:gold (treasure player))))))
      (testing "does not award more than allowed"
        (let [player (assoc player :warriors 1 :gold 5)
              player-with-beast (assoc player :warriors 1 :beast true :gold 55)]
          (is (= 6 (:gold (treasure player))))
          (is (= 56 (:gold (treasure player-with-beast))))))))
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
        (let [player (assoc (top-row-edge player :arisilon) :warriors 10 :gold 10 :brass-key true :silver-key true :gold-key true :pegasus true :sword true)]
          (is (< 10 (:gold (treasure player))))))))
  (testing "Given a player with everything and max gold, awards nothing"
    (let [player (assoc (top-row-edge player :arisilon) :warriors 10 :gold 99 :brass-key true :silver-key true :gold-key true :pegasus true :sword true)]
      (is (= player (treasure player))))))

