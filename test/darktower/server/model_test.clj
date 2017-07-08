(ns darktower.server.model-test
    (:require [clojure.test :refer :all]
              [darktower.server.model :refer :all]))

(deftest replace-player-test
  (testing "given a collection of players, replaces the entry specified by the uid"
    (let [rusty {:uid "15" :name "rusty" :kingdom :arisilon :move-count 0 :gold 10 :warriors 10}
          tanya {:uid "22" :name "tanya" :kingdom :brynthia :move-count 0 :gold 10 :warriors 10}
          players [rusty tanya]
          updated-tanya {:uid "22" :name "tanya" :kingdom :brynthia :move-count 0 :gold 8 :warriors 8}]
      (is (= [updated-tanya rusty]
             (replace-player players updated-tanya))))))
