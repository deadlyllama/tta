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
    (run-action identity-action game) => {:result game, :ok? true, :messages #{}}
    (:worker-pool (player/current-player (:result with-decreased-workers))) => 0
    fails-to-decrease-workers => {:result (:result with-decreased-workers)
                                  :ok? false
                                  :messages #{"empty worker pool"}}
    (:worker-pool (player/current-player (:result result))) => 2
    (get-in (player/current-player (:result result))
            [:buildings :mine]) => 3))

(defn game-state [result] (get result 0))
(defn action-success? [result] (get result 1))

(facts "Increasing population"
  (comment
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
                                    :commodities (contains {:food 0})}))))

(comment
  (facts "build-farm"
    (let [[insufficient-resources] (build-farm sample-game-state)
          game-without-working-pool
          (player/eventless-update-player-with
            (fn [player]
              (multi-assoc-in player
                              [:commodities :resources]
                              2
                              [:worker-pool]
                              0))
            sample-game-state)
          [insufficient-working-pool] (build-farm game-without-working-pool)
          game (player/eventless-update-player-with
                 (fn [player]
                   (assoc player :worker-pool 1))
                 game-without-working-pool)
          [sufficient-resources] (build-farm game)
          farm-count (fn [game]
                       (get-in (player/current-player game) [:buildings :farm]))
          actions-remaining (fn [game]
                              (get-in (player/current-player game) [:civil-actions :remaining]))]
      (farm-count insufficient-resources) => 2
      (actions-remaining insufficient-resources) => 4
      (farm-count insufficient-working-pool) => 2
      (farm-count sufficient-resources) => 3
      (actions-remaining sufficient-resources) => 3)))

(comment
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
  )
