(ns tta.actions
  (:require [tta.player :as player]))

(defn build-building [game building building-name]
  (player/associate-events-to-current-player
    (player/update-player-with
      (fn [player]
        (if (<= 2 (get-in player [:commodities :resources]))
          [(-> player
               (update-in [:buildings building] inc)
               (update-in [:commodities :resources] #(- % 2)))
           [(str "built a " building-name " for " 2 " resources")]
           true]
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
