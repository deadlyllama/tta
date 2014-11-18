(ns tta.game
  (:use [clojure.algo.monads :only [domonad defmonad with-monad m-chain]])
  (:require [tta.actions :as actions]
            [tta.player :as player]))

(defmonad event-m
  [m-result (fn [a-value] [a-value []])
   m-bind   (fn [[a-value events] f]
              (let [[f-value f-events] (f a-value)]
                [f-value (concat events f-events)]))])

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
    [updated-game [(str "Produced " amount " food")]]))

(defn produce-resources [game]
  (let [[updated-game amount]
          (player/update-player-with #(produce-from :mine :resources %) game)]
    [updated-game [(str "Produced " amount " resources")]]))

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
        events (-> []
                 paid-corruption-event
                 unpaid-corruption-event
                 )]
    [updated-game events]))

(defn pay-consumption [game]
  (let [[updated-game amounts] (player/update-player-with take-consumption-from game)
        paid-consumption-event (if (pos? (:paid amounts))
                                 [(str "Paid " (:paid amounts) " food in consumption.")]
                                 [])
        unpaid-consumption-event (if (pos? (:unpaid amounts))
                                   [(str (:unpaid amounts) " consumption left unpaid.")]
                                   [])
        events (concat paid-consumption-event unpaid-consumption-event)]
    [updated-game events]))

(defn reset-actions [game]
  (let [updated-game (player/assoc-in-current-player
                       game [:civil-actions :remaining] 4)]
    [updated-game []]))

(defn production-phase [game]
  (with-monad event-m
    ((m-chain [produce-food
               pay-consumption
               produce-resources
               pay-corruption
               reset-actions])
     game)))

(defn attempt-action [f game]
  (let [[after-action success] (f game)
        actions-remaining? (fn [game]
                            (pos? (get-in (player/current-player game)
                                          [:civil-actions :remaining])))
        pay-for-action (fn [game]
                         (player/update-in-current-player
                           game [:civil-actions :remaining] dec))]
    (if (actions-remaining? game)
      (if success
        (pay-for-action after-action)
        after-action)
      (player/associate-events-to-current-player
        [game ["no civil actions remaining"]]))))

(defn end-turn [game]
  (let [[updated-game events] (production-phase game)
        with-events (assoc-in updated-game
                              [:players (:current-player game) :events]
                              events)]
    (if (last-players-turn? with-events)
      (next-round with-events)
      (next-player with-events))))

