(ns tta.actions
  (:use [tta.utils :only [messageless message-m write]]
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

(defn decrease-counter [counter-path amount error-message]
  {:requirements
     #{(fn [game]
         (if (<= amount
                 (player/get-in-current-player game counter-path))
           {:ok? true
            :messages []}
           {:ok? false
            :messages [error-message]}))}
   :action (messageless
             (fn [game]
               (player/update-current-player game counter-path #(- % amount))))})

(defn increase-counter [counter-path amount]
  {:requirements #{}
   :action (messageless
             (fn [game]
               (player/update-current-player game counter-path #(+ % amount))))})

(def decrease-worker-pool
  (decrease-counter [:worker-pool] 1 "empty worker pool"))

(def decrease-population-pool
  (decrease-counter [:population-bank] 1 "empty population pool"))

(def increase-worker-pool
  (increase-counter [:worker-pool] 1))

(def increase-mines
  (increase-counter [:buildings :mine] 1))

(def increase-farms
  (increase-counter [:buildings :farm] 1))

(def increase-temples
  (increase-counter [:buildings :temple] 1))

(def increase-labs
  (increase-counter [:buildings :lab] 1))

(defn decrease-resources-by [amount]
  (combine
    (decrease-counter [:commodities :resources] amount "Not enough resources.")
    (increase-counter [:supply] amount)))

(defn decrease-food-by [amount]
  (combine
    (decrease-counter [:commodities :food] amount "not enough food")
    (increase-counter [:supply] amount)))

(def pay-action
  (decrease-counter [:civil-actions :remaining] 1 "no actions remaining"))

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
                        :messages #{"Not enough food."}}))}
   :action (messageless
             (fn [game]
               (let [cost (population-increase-cost (player/current-player game))]
                 (-> game
                  (player/update-in-current-player [:commodities :food] #(- % cost))
                  (player/update-in-current-player [:supply] #(+ % cost))))))})

(defn write-message [message]
  {:requirements #{}
   :action (write message)})

(def increase-population-action
  (combine pay-population-increase-cost
           decrease-population-pool
           increase-worker-pool
           pay-action
           (write-message "Increased population.")))

(defn build-building [& {:keys [building-action resource-cost building-name]}]
  (combine
    building-action
    (decrease-resources-by resource-cost)
    pay-action
    decrease-worker-pool
    (write-message (str "Built a " building-name " for " resource-cost
                        " resources."))))

(def build-farm-action
  (build-building :building-action increase-farms
                  :resource-cost 2
                  :building-name "farm"))

(def build-mine-action
  (build-building :building-action increase-mines
                  :resource-cost 2
                  :building-name "mine"))

(def build-temple-action
  (build-building :building-action increase-temples
                  :resource-cost 3
                  :building-name "temple"))

(def build-lab-action
  (build-building :building-action increase-labs
                  :resource-cost 3
                  :building-name "lab"))
