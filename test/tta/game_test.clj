(ns tta.game-test
  (:use [midje.sweet :only [facts fact =>]]
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

(facts "event-m"
  (let [lolinc (fn [x] [(inc x) ["lol"]])]
    (domonad event-m
            [x [3 ["jee"]]
             y (lolinc x)]
            y)) => [4 ["jee" "lol"]])
