(ns tta.game
  (:use [clojure.algo.monads :only [domonad defmonad with-monad m-chain]]
        [midje.sweet :only [fact facts =>]]))

(defmonad event-m
  [m-result (fn [a-value] [a-value []])
   m-bind   (fn [[a-value events] f]
              (let [[f-value f-events] (f a-value)]
                [f-value (concat events f-events)]))])

(facts "event-m"
  (let [lolinc (fn [x] [(inc x) ["lol"]])]
    (domonad event-m
            [x [3 ["jee"]]
             y (lolinc x)]
            y)) => [4 ["jee" "lol"]])

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

(def sample-game-state
  {:players [(create-player "Laura")
             (create-player "Ilmari")
             (create-player "Juhana")]
   :current-player 0
   :current-round 1})

(defn player-count [game]
  (count (:players game)))

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

(defn production-amount [building commodity player]
  (min (:supply player)
       (get-in player [:buildings building])))

(defn produce-from [building commodity player]
  (let [amount (production-amount building commodity player)]
    [(-> player
       (update-in [:commodities commodity] #(+ % amount))
       (update-in [:supply] #(- % amount)))
     amount]))

(defn corruption [player]
  (let [supply (:supply player)]
    (cond
      (= supply 0) 6
      (< supply 5) 4
      (< supply 9) 2
      :else        0)))

(defn take-corruption-from [player]
  (let [corruption (corruption player)
        paid-corruption (min corruption
                             (get-in player [:commodities :resources]))
        unpaid-corruption (- corruption paid-corruption)]
    [(-> player
       (update-in [:commodities :resources] #(- % paid-corruption))
       (update-in [:supply] #(+ % paid-corruption)))
     {:paid paid-corruption
      :unpaid unpaid-corruption}]))

(defn singleton? [coll]
  (and (not (empty? coll))
       (empty? (rest coll))))

(defn multi-assoc-in [target & path-value-pairs]
  (if (or (empty? path-value-pairs)
          (singleton? path-value-pairs))
    target
    (let [[path value] (take 2 path-value-pairs)
          rest-pairs (drop 2 path-value-pairs)]
      (apply multi-assoc-in (assoc-in target path value)
                            rest-pairs))))

(defn update-player-with [f game]
  (let [player (current-player game)
        [updated-player data] (f player)
        updated-game (assoc-in game
                               [:players (:current-player game)]
                               updated-player)]
    [updated-game data]))

(defn produce-food [game]
  (let [[updated-game amount]
          (update-player-with #(produce-from :farm :food %) game)]
    [updated-game [(str "Produced " amount " food")]]))

(defn produce-resources [game]
  (let [[updated-game amount]
          (update-player-with #(produce-from :mine :resources %) game)]
    [updated-game [(str "Produced " amount " resources")]]))

(defn pay-corruption [game]
  (let [[updated-game amounts]
          (update-player-with take-corruption-from game)
        paid-corruption-event (fn [events]
                                (if (pos? (:paid amounts))
                                  (conj events (str "Paid " (:paid amounts) " resources in corruption."))
                                  events))
        unpaid-corruption-event (fn [events]
                                  (if (pos? (:unpaid amounts))
                                    (conj events (str (:unpaid amounts) " corruption left unpaid."))
                                    events))
        events (-> []
                 paid-corruption-event
                 unpaid-corruption-event
                 )]
    [updated-game events]))

(defn production-phase [game]
  (with-monad event-m
    ((m-chain [produce-food
               produce-resources
               pay-corruption])
     game)))

(defn end-turn [game]
  (let [[updated-game events] (production-phase game)
        with-events (assoc-in updated-game
                              [:players (:current-player game) :events]
                              events)]
    (if (last-players-turn? with-events)
      (next-round with-events)
      (next-player with-events))))

