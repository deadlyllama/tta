(ns tta.actions-test
  (:use [midje.sweet :only [facts fact => contains]]
        ;no bullshit, only  facts
        [clojure.algo.monads :only [domonad]]
        tta.actions
        tta.utils
        [tta.game :only [sample-game-state]])
  (:require [tta.player :as player]))

(facts "run action"
  (let [game sample-game-state
        with-decreased-workers (run-action decrease-worker-pool game)
        fails-to-decrease-workers (run-action decrease-worker-pool
                                              (:result with-decreased-workers))
        combined (combine increase-mines
                          increase-worker-pool)
        result (run-action combined game)
        game2 (player/assoc-in-current-player game [:commodities :resources] 3)
        with-extra-farm (run-action build-farm-action game2)]
    (run-action identity-action game) => {:result game, :ok? true, :messages []}
    (:worker-pool (player/current-player (:result with-decreased-workers))) => 0
    fails-to-decrease-workers => {:result (:result with-decreased-workers)
                                  :ok? false
                                  :messages #{"empty worker pool"}}
    (:worker-pool (player/current-player (:result result))) => 2
    (player/get-in-current-player (:result result) [:buildings :mine]) => 3))

(facts "Increasing population"
  (let [game (-> sample-game-state
                 (player/assoc-in-current-player [:commodities :food] 2)
                 (player/assoc-in-current-player [:supply] 15)
                 (player/assoc-in-current-player [:population-bank] 17))
        result (run-action increase-population-action game)]

    result => (contains {:ok? true
                         :messages ["Increased population for 2 food."]})

    (player/get-in-current-player (:result result) [:supply]) => 17))

(facts "build-farm"
  (let [insufficient-resources
          (run-action build-farm-action sample-game-state)
        game-without-working-pool
          (-> sample-game-state
              (player/assoc-in-current-player [:commodities :resources] 2)
              (player/assoc-in-current-player [:worker-pool] 0))
        insufficient-working-pool
          (run-action build-farm-action game-without-working-pool)
        game (player/assoc-in-current-player game-without-working-pool [:worker-pool] 1)
        sufficient-resources (run-action build-farm-action game)
        farm-count (fn [game]
                       (player/get-in-current-player game [:buildings :farm]))
        actions-remaining (fn [game]
                              (player/get-in-current-player
                                game [:civil-actions :remaining]))]
    (farm-count (:result insufficient-resources)) => 2
    (actions-remaining (:result insufficient-resources)) => 4
    (farm-count (:result insufficient-working-pool)) => 2
    (farm-count (:result sufficient-resources)) => 3
    (actions-remaining (:result sufficient-resources)) => 3))

(facts "build-mine"
  (let [insufficient-resources (run-action build-mine-action sample-game-state)
        game (player/assoc-in-current-player
               sample-game-state
               [:commodities :resources]
               2)
        sufficient-resources (run-action build-mine-action game)
        mine-count (fn [game]
                     (player/get-in-current-player game [:buildings :mine]))]
    (mine-count (:result insufficient-resources)) => 2
    (:messages insufficient-resources) => #{"Not enough resources."}
    (mine-count (:result sufficient-resources)) => 3
    (:messages sufficient-resources) => ["Built a mine for 2 resources."]))
