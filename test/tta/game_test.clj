(ns tta.game-test
  (:use [midje.sweet :only [facts fact => contains]]
        ;no bullshit, only  facts
        [clojure.algo.monads :only [domonad]]
        tta.game))

(fact
  (:name (create-player "Juhana")) => "Juhana")

(fact (player-count sample-game-state) => 3)

(fact "multi-assoc-in"
  (let [player (current-player sample-game-state)]
    (multi-assoc-in player [:commodities :resources] 2
                           [:supply]                 4) =>
    (-> player (assoc-in [:commodities :resources] 2)
               (assoc-in [:supply]                 4)))
  (multi-assoc-in {:key 1} [:key] 2 [:key] 3) => {:key 3})

(def game-data 0)
(def event-data 1)

(fact "Corruption reduces resources and increases supply"
  (let [player (current-player sample-game-state)
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
      (let [player (current-player sample-game-state)
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

(fact
  (get-in (produce-food sample-game-state)
          [game-data :players 0 :commodities :food]) => 2
  (get-in (produce-food (assoc sample-game-state :current-player 1))
          [game-data :players 1 :commodities :food]) => 2
  (get-in (produce-food (get (produce-food sample-game-state) game-data))
          [game-data :players 0 :commodities :food]) => 4)

(fact
  (get-in (produce-resources sample-game-state)
          [game-data :players 0 :commodities :resources]) => 2
  (get-in (produce-resources (assoc sample-game-state :current-player 1))
          [game-data :players 1 :commodities :resources]) => 2
  (get-in (produce-resources (get (produce-resources sample-game-state) game-data))
          [game-data :players 0 :commodities :resources]) => 4)

(fact "Production reduces supply"
  (get-in (produce-food sample-game-state)
          [game-data :players 0 :supply]) => 16
  (get-in (produce-resources sample-game-state)
          [game-data :players 0 :supply]) => 16
  (get-in (produce-food (get (produce-food sample-game-state) game-data))
          [game-data :players 0 :supply]) => 14
  (get-in (produce-resources (get (produce-resources sample-game-state) game-data))
          [game-data :players 0 :supply]) => 14
  (fact "Production cannot exceed supply"
    (let [player (multi-assoc-in (current-player sample-game-state)
                                 [:supply] 1
                                 [:commodities :food] 0)
          game-state (get (produce-food
                            (assoc-in sample-game-state
                                      [:players (:current-player sample-game-state)]
                                      player))
                          game-data)]
        (:supply (current-player game-state)) => 0
        (get-in (current-player game-state)
                [:commodities :food]) => 1)))

(fact "ending a turn rotates to next player,
       and updates current round after last player"
  (let [game sample-game-state
        game2 (assoc game :current-player 2)]
    (:current-player (end-turn game)) => 1
    (:current-player (end-turn game2)) => 0
    (:current-round (end-turn game2)) => 2))

(fact "eventless-update-player-with"
  (->> sample-game-state
       (eventless-update-player-with (fn [player]
                                       (assoc player :population-bank 5)))
       current-player
       :population-bank)
  => 5)

(facts "Increasing population"
  (let [game (assoc-in sample-game-state
                       [:players (:current-player sample-game-state) :commodities :food]
                       3)
        game2 (eventless-update-player-with
                (fn [player]
                  (multi-assoc-in player
                                  [:commodities :food] 3
                                  [:worker-pool] 3
                                  [:population-bank] 16))
                sample-game-state)
        updated-player (current-player (get (increase-population-action game) game-data))
        updated-player2 (current-player (get (increase-population-action game2) game-data))]
    updated-player => (contains {:worker-pool 2, :population-bank 17
                                 :commodities (contains {:food 1})})
    updated-player2 => (contains {:worker-pool 4, :population-bank 15
                                  :commodities (contains {:food 0})})))

(fact "Cannot increase population with empty population bank"
  (let [game (eventless-update-player-with
               (fn [player]
                 (multi-assoc-in player
                                 [:commodities :food] 10
                                 [:worker-pool] 0
                                 [:population-bank] 0 ))
               sample-game-state)]
    (current-player (get (increase-population-action game) game-data))
      => (contains {:worker-pool 0, :population-bank 0
                    :commodities (contains {:food 10})})))

(fact "Cannot increase population without sufficient food"
  (let [updated-player (current-player (get (increase-population-action sample-game-state)
                                            game-data))]
    (:worker-pool updated-player) => 1))

(facts "event-m"
  (let [lolinc (fn [x] [(inc x) ["lol"]])]
    (domonad event-m
            [x [3 ["jee"]]
             y (lolinc x)]
            y)) => [4 ["jee" "lol"]])

(facts "build-farm"
  (let [insufficient-resources (build-farm sample-game-state)
        game (eventless-update-player-with
               (fn [player]
                 (assoc-in player
                           [:commodities :resources]
                           2))
               sample-game-state)
        sufficient-resources (build-farm game)
        farm-count (fn [game]
                     (get-in (current-player game) [:buildings :farm]))]
    (farm-count insufficient-resources) => 2
    (farm-count sufficient-resources) => 3))

(facts "build-mine"
  (let [insufficient-resources (build-mine sample-game-state)
        game (eventless-update-player-with
               (fn [player]
                 (assoc-in player
                           [:commodities :resources]
                           2))
               sample-game-state)
        sufficient-resources (build-mine game)
        mine-count (fn [game]
                     (get-in (current-player game) [:buildings :mine]))]
    (mine-count insufficient-resources) => 2
    (mine-count sufficient-resources) => 3))
