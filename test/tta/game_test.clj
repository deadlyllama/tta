(ns tta.game-test
  (:use [midje.sweet :only [facts fact =>]]
        ;no bullshit, only  facts
        tta.game))

(fact
  (:name (create-player "Juhana")) => "Juhana")

(fact (player-count sample-game-state) => 3)

(fact
  (let [player (current-player sample-game-state)]
    (unpaid-corruption (multi-assoc-in player
                                       [:supply] 8
                                       [:commodities :food] 9
                                       [:commodities :resources] 1))
      => 1
    (unpaid-corruption (multi-assoc-in player
                                       [:supply] 8
                                       [:commodities :resources] 10))
      => 0
    (unpaid-corruption (multi-assoc-in player
                                       [:supply] 4
                                       [:commodities :food] 12
                                       [:commodities :resources] 2))
      => 2))

(fact "multi-assoc-in"
  (let [player (current-player sample-game-state)]
    (multi-assoc-in player [:commodities :resources] 2
                           [:supply]                 4) =>
    (-> player (assoc-in [:commodities :resources] 2)
               (assoc-in [:supply]                 4)))
  (multi-assoc-in {:key 1} [:key] 2 [:key] 3) => {:key 3})

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
    (resources (pay-corruption player-without-corruption))        => 2
    (resources (pay-corruption player-with-light-corruption))     => 8
    (resources (pay-corruption player-with-medium-corruption))    => 10
    (resources (pay-corruption player-with-heavy-corruption))     => 12
    (resources (pay-corruption player-who-cannot-pay-corruption)) => 0
    (:supply (pay-corruption player-without-corruption))        => 16
    (:supply (pay-corruption player-with-light-corruption))     => 10
    (:supply (pay-corruption player-with-medium-corruption))    => 8
    (:supply (pay-corruption player-with-heavy-corruption))     => 6
    (:supply (pay-corruption player-who-cannot-pay-corruption)) => 3))

(fact "Production phase produces food"
  (get-in (production-phase sample-game-state)
          [:players 0 :commodities :food]) => 2
  (get-in (production-phase (assoc sample-game-state :current-player 1))
          [:players 1 :commodities :food]) => 2
  (get-in (production-phase (production-phase sample-game-state))
          [:players 0 :commodities :food]) => 4)

(fact "Production phase produces resources"
  (get-in (production-phase sample-game-state)
          [:players 0 :commodities :resources]) => 2
  (get-in (production-phase (assoc sample-game-state :current-player 1))
          [:players 1 :commodities :resources]) => 2
  (get-in (production-phase (production-phase sample-game-state))
          [:players 0 :commodities :resources]) => 4)

(fact "Production reduces supply"
  (get-in (production-phase sample-game-state)
          [:players 0 :supply]) => 14
  (get-in (production-phase (production-phase sample-game-state))
          [:players 0 :supply]) => 10
  (fact "Production cannot exceed supply"
    (let [player (multi-assoc-in (current-player sample-game-state)
                                 [:supply] 1
                                 [:commodities :food] 0)
          game-state (production-phase
                       (assoc-in sample-game-state
                                 [:players (:current-player sample-game-state)]
                                 player))]
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