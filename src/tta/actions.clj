(ns tta.actions
  (:require [tta.player :as player]))

(defn decrease-resource [game resource-path amount error-message]
  (if (<= amount
          (get-in (player/current-player game) resource-path) amount)
    {:result (player/update-current-player
               game resource-path #(- % amount))
     :succeed? true
     :messages []}
    {:result game
     :succeed? false
     :messages [error-message]}))

(defn increase-resource [game resource-path amount]
  {:result (player/update-current-player
             game resource-path #(+ % amount))
   :succeed? true
   :messages []})

(defn decrease-worker-pool [game]
  (decrease-resource game [:worker-pool] 1 "empty worker pool"))

(defn decrease-population-pool [game]
  (decrease-resource game [:population-pool] 2 "empty population pool"))

(defn increase-worker-pool [game]
  (increase-resource game [:worker-pool] 1))

(defn increase-mines [game]
  (increase-resource game [:buildings :mine] 1))

(defn combine [action & actions]
  (fn [game]
    (if (empty? actions)
      (action game)
      (let [result (action game)]
        (if (:succeed? result)
          (let [combined (apply combine actions)
                result2 (combined (:result result))]
            (if (:succeed? result2)
              result2
              {:result game
               :succeed? false
               :messages (:messages result2)}))
          {:result game
           :succeed? false
           :messages (:messages result)})))))

(defn build-building [game building building-name]
  (player/associate-events-to-current-player
    (player/update-player-with
      (fn [player]
        (cond (and (<= 2 (get-in player [:commodities :resources]))
                   (<= 1 (:worker-pool player)))
                [(-> player
                   (update-in [:buildings building] inc)
                   (update-in [:commodities :resources] #(- % 2))
                   (update-in [:civil-actions :remaining] dec))
                [(str "built a " building-name " for " 2 " resources")]
                true]
              (<= 2 (get-in player [:commodities :resources]))
                [player
                 ["no workers in worker pool"]
                 false]
              :else
                [player
                 [(str "not enough resources to build a " building-name)]
                 false]))
      game)))

(defn build-farm [game]
  (build-building game :farm "farm"))

(defn build-mine [game]
  (build-building game :mine "mine"))

(defn population-increase-cost [a-player]
  (let [bank (:population-bank a-player)]
    (cond (< bank 5)  7
          (< bank 9)  5
          (< bank 13) 4
          (< bank 17) 3
          :else       2)))

(defn increase-population [game]
  (player/associate-events-to-current-player
    (let [current-player (player/current-player game)
          cost (population-increase-cost current-player)]
      (cond (< (get-in current-player [:commodities :food]) cost)
              [game
               ["Can't increase population without sufficient food"]
               false]
            (zero? (:population-bank current-player))
              [game
               ["Can't increase population with empty population bank"]
               false]
            :else
              [(-> game
                   (player/update-current-player [:worker-pool] inc)
                   (player/update-current-player [:population-bank] dec)
                   (player/update-current-player [:commodities :food] #(- % cost)))
               [(str "Increased population for " cost " food")]
               true]))))
