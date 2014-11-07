(ns tta.utils-test
  (:use [midje.sweet :only [facts fact => contains]]
        ;no bullshit, only  facts
        tta.utils
        [tta.game :only [sample-game-state]])
  (:require [tta.player :as player]))

(fact "multi-assoc-in"
  (let [player (player/current-player sample-game-state)]
    (multi-assoc-in player [:commodities :resources] 2
                           [:supply]                 4) =>
    (-> player (assoc-in [:commodities :resources] 2)
               (assoc-in [:supply]                 4)))
  (multi-assoc-in {:key 1} [:key] 2 [:key] 3) => {:key 3})

