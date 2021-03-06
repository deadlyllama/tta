(ns tta.game
  (:use [clojure.algo.monads :only [domonad defmonad with-monad m-chain]]
        [tta.utils :only [message-m no-messages]])
  (:require [tta.actions :as actions]
            [tta.player :as player]))

(def sample-game-state
  {:players [(player/create-player "Laura")
             (player/create-player "Ilmari")
             (player/create-player "Juhana")]
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

(defn consumption [player]
  (let [pop (:population-bank player)]
    (cond
     (= pop 0)  6
     (< pop 5)  4
     (< pop 9)  3
     (< pop 13) 2
     (< pop 17) 1
     :else      0)))

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

(defn take-consumption-from [player]
  (let [consumption (consumption player)
        paid-consumption (min consumption
                              (get-in player [:commodities :food]))
        unpaid-consumption (- consumption paid-consumption)]
    [(-> player
         (update-in [:commodities :food] #(- % paid-consumption))
         (update-in [:supply] #(+ % paid-consumption)))
     {:paid paid-consumption
      :unpaid unpaid-consumption}]))

(defn produce-food [game]
  (let [[updated-game amount]
          (player/update-player-with #(produce-from :farm :food %) game)]
    {:result updated-game
     :messages [(str "Produced " amount " food.")]}))

(defn produce-resources [game]
  (let [[updated-game amount]
          (player/update-player-with #(produce-from :mine :resources %) game)]
    {:result updated-game
     :messages [(str "Produced " amount " resources.")]}))

(defn pay-corruption [game]
  (let [[updated-game amounts]
          (player/update-player-with take-corruption-from game)
        paid-corruption-event (fn [events]
                                (if (pos? (:paid amounts))
                                  (conj events (str "Paid " (:paid amounts) " resources in corruption."))
                                  events))
        unpaid-corruption-event (fn [events]
                                  (if (pos? (:unpaid amounts))
                                    (conj events (str (:unpaid amounts) " corruption left unpaid."))
                                    events))
        messages (-> []
                   paid-corruption-event
                   unpaid-corruption-event
                   )]
    {:result updated-game
     :messages messages}))

(defn pay-consumption [game]
  (let [[updated-game amounts] (player/update-player-with take-consumption-from game)
        paid-with-food (:paid amounts)
        unpaid (:unpaid amounts)
        culture-to-pay (min (* unpaid 4)
                            (player/get-in-current-player updated-game [:culture]))
        after-culture-payment (player/update-in-current-player
                                updated-game [:culture] #(- % culture-to-pay))
        food-consumption-event (if (pos? paid-with-food)
                                 [(str "Paid " paid-with-food " food in consumption.")]
                                 [])
        culture-consumption-event (if (pos? culture-to-pay)
                                   [(str culture-to-pay " culture paid in consumption.")]
                                   [])
        messages (concat food-consumption-event culture-consumption-event)]
    {:result after-culture-payment
     :messages messages}))

(defn reset-actions [game]
  (let [updated-game (player/assoc-in-current-player
                       game
                       [:civil-actions :remaining]
                       (player/get-in-current-player game [:civil-actions :total]))]
    (no-messages updated-game)))

(defn produce-culture [game]
  (let [temples (player/get-in-current-player game [:buildings :temple])]
    {:result (player/update-in-current-player game [:culture] #(+ % temples))
     :messages [(str "Produced " temples " culture." )]}))

(defn produce-science [game]
  (let [labs (player/get-in-current-player game [:buildings :lab])]
    {:result (player/update-in-current-player game [:science] #(+ % labs))
     :messages [(str "Produced " labs " science.")]}))

(defn production-phase [game]
  (with-monad message-m
    ((m-chain [produce-culture
               produce-science
               produce-food
               pay-consumption
               produce-resources
               pay-corruption
               reset-actions])
     game)))

(defn attempt-action [action game]
  (let [result (actions/run-action action game)]
    (player/associate-events-to-current-player
      (:result result)
      (:messages result))))

(defn end-turn [game]
  (let [{updated-game :result, messages :messages} (production-phase game)
        with-events (assoc-in updated-game
                              [:players (:current-player game) :events]
                              messages)]
    (if (last-players-turn? with-events)
      (next-round with-events)
      (next-player with-events))))
