(ns darktower.main
  (:require
    [darktower.autoinit]
    [darktower.view :as view]
    [darktower.views.board :as views.board]
    [reagent.core :as reagent]))

(reagent/render-component
  [view/main]
  (. js/document (getElementById "app")))
