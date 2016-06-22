(ns darktower.main
  (:require
    [darktower.autoinit]
    [darktower.view :as view]
    [reagent.core :as reagent]))

(reagent/render-component
  [view/main]
  (. js/document (getElementById "app")))
