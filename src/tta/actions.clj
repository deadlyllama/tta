(ns tta.actions
  (:require [tta.player :as player]
            [clojure.set :as set]))

(defn- combine2 [action1 action2]
  {:requirements (set/union (:requirements action1)
                            (:requirements action2))
   :action (comp (:action action1)
                 (:action action2))})

(def identity-action
  {:requirements #{}
   :action identity})

(defn combine [& actions]
  (reduce combine2 identity-action actions))

(defn run-action [action game]
  (let [requirement-results (map #(% game)
                                 (:requirements action))
        failing (filter (complement :ok?) requirement-results)
        failing-messages (set (mapcat :messages failing))]
    (if (empty? failing)
      {:result ((:action action) game)
       :ok? true
       :messages #{}}
      {:result game
       :ok? false
       :messages failing-messages})))

(defn decrease-resource [resource-path amount error-message]
  {:requirements
     #{(fn [game]
         (println resource-path)
         (if (<= amount
                 (get-in (player/current-player game) resource-path))
           {:ok? true
            :messages []}
           {:ok? false
            :messages [error-message]}))}
   :action (fn [game]
             (player/update-current-player
               game resource-path #(- % amount)))})

(defn increase-resource [resource-path amount]
  {:requirements #{}
   :action (fn [game]
             (player/update-current-player
               game resource-path #(+ % amount)))})

(def decrease-worker-pool
  (decrease-resource [:worker-pool] 1 "empty worker pool"))

(def decrease-population-pool
  (decrease-resource [:population-bank] 1 "empty population pool"))

(def increase-worker-pool
  (increase-resource [:worker-pool] 1))

(def increase-mines
  (increase-resource [:buildings :mine] 1))

(def increase-farms
  (increase-resource [:buildings :farm] 1))

(defn decrease-resources-by [amount]
  (decrease-resource [:commodities :resources] amount "not enough resources"))

(defn decrease-food-by [amount]
  (decrease-resource [:commodities :food] amount "not enough food"))

(def pay-action
  (decrease-resource [:civil-actions :remaining] 1 "no actions remaining"))

(defn population-increase-cost [a-player]
  (let [bank (:population-bank a-player)]
    (cond (< bank 5)  7
          (< bank 9)  5
          (< bank 13) 4
          (< bank 17) 3
          :else       2)))

(def pay-population-increase-cost
  {:requirements #{(fn [game]
                     (if (<= (population-increase-cost (player/current-player game))
                             (get-in (player/current-player game) [:commodities :food]))
                       {:ok? true
                        :messages #{}}
                       {:ok? false
                        :messages #{"not enough food"}}))}
   :action (fn [game]
             (player/update-current-player
               game
               [:commodities :food]
               #(- % (population-increase-cost (player/current-player game)))))})

(def increase-population-action
  (combine decrease-population-pool
           increase-worker-pool
           pay-action
           pay-population-increase-cost))

(def build-farm-action
  (combine 
    increase-farms
    (decrease-resources-by 2)
    pay-action
    decrease-worker-pool
    ))

(def build-mine-action
  (combine increase-mines
           (decrease-resources-by 2)
           pay-action
           decrease-worker-pool))
