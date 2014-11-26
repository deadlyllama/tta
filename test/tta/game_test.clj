(ns tta.game-test
  (:use [midje.sweet :only [facts fact => contains]]
        ;no bullshit, only  facts
        [clojure.algo.monads :only [domonad]]
        tta.game
        tta.utils)
  (:require [tta.player :as player]))

(fact (player-count sample-game-state) => 3)

(def game-data 0)
(def event-data 1)

(fact "Corruption reduces resources and increases supply"
  (let [player (player/current-player sample-game-state)
        player-with-resources-and-supply
          (fn [resources supply]
            (multi-assoc-in player
                            [:commodities :resources] resources
                            [:supply]                 supply))
        resources (fn [player] (get-in player [:commodities :resources]))
        player-without-corruption     (player-with-resources-and-supply  2 16)
        player-with-light-corruption  (player-with-resources-and-supply 10  8)
        player-with-medium-corruption (player-with-resources-and-supply 14  4)
        player-with-heavy-corruption  (player-with-resources-and-supply 18  0)
        player-who-cannot-pay-corruption (multi-assoc-in player
                                                         [:commodities :food] 15
                                                         [:commodities :resources] 1
                                                         [:supply] 2)]
    (resources (get (take-corruption-from player-without-corruption) game-data))        => 2
    (resources (get (take-corruption-from player-with-light-corruption) game-data))     => 8
    (resources (get (take-corruption-from player-with-medium-corruption) game-data))    => 10
    (resources (get (take-corruption-from player-with-heavy-corruption) game-data))     => 12
    (resources (get (take-corruption-from player-who-cannot-pay-corruption) game-data)) => 0
    (:paid   (get (take-corruption-from player-with-light-corruption) event-data))      => 2
    (:paid   (get (take-corruption-from player-with-medium-corruption) event-data))     => 4
    (:paid   (get (take-corruption-from player-with-heavy-corruption) event-data))      => 6
    (:unpaid (get (take-corruption-from player-with-light-corruption) event-data))      => 0
    (:unpaid (get (take-corruption-from player-with-medium-corruption) event-data))     => 0
    (:unpaid (get (take-corruption-from player-with-heavy-corruption) event-data))      => 0
    (:unpaid (get (take-corruption-from player-who-cannot-pay-corruption) event-data))  => 3
    (:supply (get (take-corruption-from player-without-corruption) game-data))          => 16
    (:supply (get (take-corruption-from player-with-light-corruption) game-data))       => 10
    (:supply (get (take-corruption-from player-with-medium-corruption) game-data))      => 8
    (:supply (get (take-corruption-from player-with-heavy-corruption) game-data))       => 6
    (:supply (get (take-corruption-from player-who-cannot-pay-corruption) game-data))   => 3))

(fact "Consumption reduces food and increases supply"
      (let [player (player/current-player sample-game-state)
            player-with
            (fn [& {:keys [supply food population-bank]}]
              (multi-assoc-in player
                              [:commodities :food] food
                              [:supply]            supply
                              [:population-bank]   population-bank))
            food (fn [player] (get-in player [:commodities :food]))
            player-without-consumption     (player-with :food 2 :supply 16 :population-bank 18)
            player-with-light-consumption  (player-with :food 10 :supply 8 :population-bank 16)
            player-with-medium-consumption (player-with :food 14 :supply 4 :population-bank 12)
            player-with-heavy-consumption  (player-with :food 18 :supply 0 :population-bank 8)
            player-with-very-heavy-consumption (player-with :food 18 :supply 0 :population-bank 4)
            player-with-extreme-consumption (player-with :food 18 :supply 0 :population-bank 0)
            player-who-cannot-pay-consumption (player-with :food 0 :supply 0 :population-bank 16)]
        (food (get (take-consumption-from player-without-consumption) game-data))        => 2
        (food (get (take-consumption-from player-with-light-consumption) game-data))     => 9
        (food (get (take-consumption-from player-with-medium-consumption) game-data))    => 12
        (food (get (take-consumption-from player-with-heavy-consumption) game-data))     => 15
        (food (get (take-consumption-from player-with-very-heavy-consumption) game-data))=> 14
        (food (get (take-consumption-from player-with-extreme-consumption) game-data))   => 12
        (food (get (take-consumption-from player-who-cannot-pay-consumption) game-data)) => 0
        (:paid   (get (take-consumption-from player-with-light-consumption) event-data))      => 1
        (:paid   (get (take-consumption-from player-with-medium-consumption) event-data))     => 2
        (:paid   (get (take-consumption-from player-with-heavy-consumption) event-data))      => 3
        (:paid   (get (take-consumption-from player-with-very-heavy-consumption) event-data)) => 4
        (:paid   (get (take-consumption-from player-with-extreme-consumption) event-data))    => 6
        (:unpaid (get (take-consumption-from player-with-light-consumption) event-data))      => 0
        (:unpaid (get (take-consumption-from player-with-medium-consumption) event-data))     => 0
        (:unpaid (get (take-consumption-from player-with-heavy-consumption) event-data))      => 0
        (:unpaid (get (take-consumption-from player-with-very-heavy-consumption) event-data)) => 0
        (:unpaid (get (take-consumption-from player-with-extreme-consumption) event-data))    => 0
        (:unpaid (get (take-consumption-from player-who-cannot-pay-consumption) event-data))  => 1
        (:supply (get (take-consumption-from player-without-consumption) game-data))          => 16
        (:supply (get (take-consumption-from player-with-light-consumption) game-data))       => 9
        (:supply (get (take-consumption-from player-with-medium-consumption) game-data))      => 6
        (:supply (get (take-consumption-from player-with-heavy-consumption) game-data))       => 3
        (:supply (get (take-consumption-from player-who-cannot-pay-consumption) game-data))   => 0))

(fact "produce-food"
  (get-in (produce-food sample-game-state)
          [:result :players 0 :commodities :food]) => 2
  (get-in (produce-food (assoc sample-game-state :current-player 1))
          [:result :players 1 :commodities :food]) => 2
  (get-in (produce-food (get (produce-food sample-game-state) :result))
          [:result :players 0 :commodities :food]) => 4)

(fact "produce-resources"
  (get-in (produce-resources sample-game-state)
          [:result :players 0 :commodities :resources]) => 2
  (get-in (produce-resources (assoc sample-game-state :current-player 1))
          [:result :players 1 :commodities :resources]) => 2
  (get-in (produce-resources (get (produce-resources sample-game-state) :result))
          [:result :players 0 :commodities :resources]) => 4)

(fact "Production reduces supply"
  (get-in (produce-food sample-game-state)
          [:result :players 0 :supply]) => 16
  (get-in (produce-resources sample-game-state)
          [:result :players 0 :supply]) => 16
  (get-in (produce-food (get (produce-food sample-game-state) :result))
          [:result :players 0 :supply]) => 14
  (get-in (produce-resources (get (produce-resources sample-game-state) :result))
          [:result :players 0 :supply]) => 14
  (fact "Production cannot exceed supply"
    (let [player (multi-assoc-in (player/current-player sample-game-state)
                                 [:supply] 1
                                 [:commodities :food] 0)
          game-state (get (produce-food
                            (assoc-in sample-game-state
                                      [:players (:current-player sample-game-state)]
                                      player))
                          :result)]
        (:supply (player/current-player game-state)) => 0
        (get-in (player/current-player game-state)
                [:commodities :food]) => 1)))

(fact "ending a turn rotates to next player,
       and updates current round after last player"
  (let [game sample-game-state
        game2 (assoc game :current-player 2)]
    (:current-player (end-turn game)) => 1
    (:current-player (end-turn game2)) => 0
    (:current-round (end-turn game2)) => 2))

(fact "ending a turn resets actions"
  (let [game (player/assoc-in-current-player
               sample-game-state [:civil-actions :remaining] 2)]
    (get-in (end-turn game)
            [:players 0 :civil-actions :remaining]) => 4))

(fact "ending a turn produces culture"
  (let [after-turn (end-turn sample-game-state)]
    (get-in after-turn [:players 0 :culture]) => 1)
  (let [with-two-temples (player/assoc-in-current-player
                           sample-game-state
                           [:buildings :temple]
                           2)
        after-turn (end-turn with-two-temples)]
    (get-in after-turn [:players 0 :culture]) => 2))
