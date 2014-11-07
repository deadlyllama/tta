(ns tta.actions-test
  (:use [midje.sweet :only [facts fact => contains]]
        ;no bullshit, only  facts
        [clojure.algo.monads :only [domonad]]
        tta.actions
        tta.utils
        [tta.game :only [sample-game-state]])
  (:require [tta.player :as player]))

(defn game-state [result] (get result 0))
(defn action-success? [result] (get result 1))

(facts "Increasing population"
  (let [game (assoc-in sample-game-state
                       [:players (:current-player sample-game-state) :commodities :food]
                       3)
        game2 (player/eventless-update-player-with
                (fn [player]
                  (multi-assoc-in player
                                  [:commodities :food] 3
                                  [:worker-pool] 3
                                  [:population-bank] 16))
                sample-game-state)
        updated-player (player/current-player (game-state (increase-population game)))
        updated-player2 (player/current-player (game-state (increase-population game2)))]
    updated-player => (contains {:worker-pool 2, :population-bank 17
                                 :commodities (contains {:food 1})})
    updated-player2 => (contains {:worker-pool 4, :population-bank 15
                                  :commodities (contains {:food 0})})))

(fact "Cannot increase population with empty population bank"
  (let [game (player/eventless-update-player-with
               (fn [player]
                 (multi-assoc-in player
                                 [:commodities :food] 10
                                 [:worker-pool] 0
                                 [:population-bank] 0 ))
               sample-game-state)]
    (player/current-player (game-state (increase-population game)))
      => (contains {:worker-pool 0, :population-bank 0
                    :commodities (contains {:food 10})})))

(fact "Cannot increase population without sufficient food"
  (let [updated-player (player/current-player
                         (game-state (increase-population sample-game-state)))]
    (:worker-pool updated-player) => 1))

(facts "build-farm"
  (let [[insufficient-resources] (build-farm sample-game-state)
        game (player/eventless-update-player-with
               (fn [player]
                 (assoc-in player
                           [:commodities :resources]
                           2))
               sample-game-state)
        [sufficient-resources] (build-farm game)
        farm-count (fn [game]
                     (get-in (player/current-player game) [:buildings :farm]))]
    (farm-count insufficient-resources) => 2
    (farm-count sufficient-resources) => 3))

(facts "build-mine"
  (let [insufficient-resources (game-state (build-mine sample-game-state))
        game (player/eventless-update-player-with
               (fn [player]
                 (assoc-in player
                           [:commodities :resources]
                           2))
               sample-game-state)
        sufficient-resources (game-state (build-mine game))
        mine-count (fn [game]
                     (get-in (player/current-player game) [:buildings :mine]))]
    (mine-count insufficient-resources) => 2
    (mine-count sufficient-resources) => 3))
