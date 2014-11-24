(ns tta.actions
  (:use [tta.utils :only [messageless message-m]]
        [clojure.algo.monads :only [with-monad m-chain]])
  (:require [tta.player :as player]
            [clojure.set :as set]))

(defn- combine2 [action1 action2]
  {:requirements (set/union (:requirements action1)
                            (:requirements action2))
   :action (with-monad message-m
             (m-chain [(:action action1)
                       (:action action2)]))})

(def identity-action
  {:requirements #{}
   :action (messageless identity)})

(defn combine [& actions]
  (reduce combine2 identity-action actions))

(defn run-action [action game]
  (let [requirement-results (map #(% game)
                                 (:requirements action))
        failing (filter (complement :ok?) requirement-results)
        failing-messages (set (mapcat :messages failing))]
    (if (empty? failing)
      (let [result ((:action action) game)]
        {:result (:result result)
         :ok? true
         :messages (:messages result)})
      {:result game
       :ok? false
       :messages failing-messages})))

(defn decrease-resource [resource-path amount error-message]
  {:requirements
     #{(fn [game]
         (println resource-path)
         (if (<= amount
                 (player/get-in-current-player game resource-path))
           {:ok? true
            :messages []}
           {:ok? false
            :messages [error-message]}))}
   :action (messageless
             (fn [game]
               (player/update-current-player
                 game resource-path #(- % amount))))})

(defn increase-resource [resource-path amount]
  {:requirements #{}
   :action (messageless
             (fn [game]
               (player/update-current-player
                 game resource-path #(+ % amount))))})

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
                             (player/get-in-current-player game [:commodities :food]))
                       {:ok? true
                        :messages #{}}
                       {:ok? false
                        :messages #{"not enough food"}}))}
   :action (messageless
             (fn [game]
               (player/update-current-player
                 game
                 [:commodities :food]
                 #(- % (population-increase-cost (player/current-player game))))))})

(defn write-message [message]
  {:requirements #{}
   :action (fn [a-value]
             {:result a-value
              :messages [message]})})

(def increase-population-action
  (combine decrease-population-pool
           increase-worker-pool
           pay-action
           pay-population-increase-cost
           (write-message "Increased population.")))

(def build-farm-action
  (combine 
    increase-farms
    (decrease-resources-by 2)
    pay-action
    decrease-worker-pool
    (write-message "Built a farm.")))

(def build-mine-action
  (combine increase-mines
           (decrease-resources-by 2)
           pay-action
           decrease-worker-pool
           (write-message "Built a mine.")))
