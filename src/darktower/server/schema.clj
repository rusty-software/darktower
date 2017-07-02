(ns darktower.server.schema
  (:require [schema.core :as s]))

(def Territory
  {:kingdom s/Keyword
   (s/optional-key :row) s/Int
   (s/optional-key :idx) s/Int
   (s/optional-key :type) s/Keyword})

(def Player
  {:uid s/Str
   :name s/Str
   :multiplayer? s/Bool
   :kingdom s/Keyword
   :warriors s/Int
   :gold s/Int
   :food s/Int
   :scout s/Bool
   :healer s/Bool
   :beast s/Bool
   :brass-key s/Bool
   :silver-key s/Bool
   :gold-key s/Bool
   :pegasus s/Bool
   :sword s/Bool
   :current-territory Territory
   :move-count s/Int
   (s/optional-key :last-territory) Territory
   (s/optional-key :encounter-result) s/Keyword
   (s/optional-key :extra-turn) s/Bool})
