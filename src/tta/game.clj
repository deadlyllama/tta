(ns tta.game
  (:use [midje.sweet :only [fact facts =>]]))

(def initial-player-state
  {:buildings {:temple 1
               :farm 2
               :mine 2}
   :commodities {:food 0
                 :resources 0}
   :supply 18})

(defn create-player [a-name]
  (assoc initial-player-state
         :name a-name))

(fact
  (:name (create-player "Juhana")) => "Juhana")

(def sample-game-state
  {:players [(create-player "Laura")
             (create-player "Ilmari")
             (create-player "Juhana")]
   :current-player 0
   :current-round 1})

(defn player-count [game]
  (count (:players game)))

(fact (player-count sample-game-state) => 3)

(defn last-players-turn? [game]
  (= (dec (player-count game))
     (:current-player game)))

(defn next-player [game]
  (update-in game [:current-player] inc))

(defn next-round [game]
  (assoc game :current-player 0
              :current-round (inc (:current-round game))))

(defn current-player [game]
  (get (:players game)
       (:current-player game)))

(defn produce-from [player building commodity]
  (let [amount (min (:supply player)
                    (get-in player [:buildings building]))]
    (-> player
      (update-in [:commodities commodity] #(+ % amount))
      (update-in [:supply] #(- % amount)))))

(defn corruption [player]
  (let [supply (:supply player)]
    (cond
      (= supply 0) 6
      (< supply 5) 4
      (< supply 9) 2
      :else        0)))

(defn pay-corruption [player]
  (let [corruption (min (corruption player) (get-in player [:commodities :resources]))]
    (-> player
      (update-in [:commodities :resources] #(- % corruption))
      (update-in [:supply] #(+ % corruption)))))

(fact "Corruption reduces resources and increases supply"
  (let [player (current-player sample-game-state)
        player-without-corruption (-> player
                                    (assoc-in [:commodities :resources] 2)
                                    (assoc :supply 16))
        player-with-light-corruption (-> player
                                       (assoc-in [:commodities :resources] 10)
                                       (assoc :supply 8))
        player-with-medium-corruption (-> player
                                        (assoc-in [:commodities :resources] 14)
                                        (assoc :supply 4))
        player-with-heavy-corruption (-> player
                                       (assoc-in [:commodities :resources] 18)
                                       (assoc :supply 0))
        player-who-cannot-pay-corruption (-> player
                                           (assoc-in [:commodities :food] 15)
                                           (assoc-in [:commodities :resources] 1)
                                           (assoc :supply 2))]
    (get-in (pay-corruption player-without-corruption)
            [:commodities :resources]) => 2
    (get-in (pay-corruption player-with-light-corruption)
            [:commodities :resources]) => 8
    (get-in (pay-corruption player-with-medium-corruption)
            [:commodities :resources]) => 10
    (get-in (pay-corruption player-with-heavy-corruption)
            [:commodities :resources]) => 12
    (get-in (pay-corruption player-who-cannot-pay-corruption)
            [:commodities :resources]) => 0
    (:supply (pay-corruption player-without-corruption)) => 16
    (:supply (pay-corruption player-with-light-corruption)) => 10
    (:supply (pay-corruption player-with-medium-corruption)) => 8
    (:supply (pay-corruption player-with-heavy-corruption)) => 6
    (:supply (pay-corruption player-who-cannot-pay-corruption)) => 3))

(defn production-phase [game]
  "Updates the current player's board state according to the rules of the
  production phase."
  (let [player (current-player game)
        updated-player (-> player
                         (produce-from :farm :food)
                         (produce-from :mine :resources))]
    (assoc-in game [:players (:current-player game)] updated-player)))

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
          [:players 0 :supply]) => 10)

(defn pass [game]
  (let [updated-game (production-phase game)]
    (if (last-players-turn? updated-game)
      (next-round updated-game)
      (next-player updated-game))))

(let [game sample-game-state
      game2 (assoc game :current-player 2)]
  (fact "passing rotates to next player,
         and updates current round after last player"
    (:current-player (pass game)) => 1
    (:current-player (pass game2)) => 0
    (:current-round (pass game2)) => 2))

