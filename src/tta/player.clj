(ns tta.player)

(def initial-player-state
  {:buildings {:temple 1
               :farm 2
               :mine 2}
   :commodities {:food 0
                 :resources 0}
   :population-bank 18
   :civil-actions {:total 4, :remaining 4}
   :worker-pool 1
   :supply 18})

(defn create-player [a-name]
  (assoc initial-player-state
         :name a-name))

(defn current-player [game]
  (get (:players game)
       (:current-player game)))

(defn eventless-update-player-with [f game]
  (update-in game [:players (:current-player game)] f))

(defn update-in-current-player [game path f]
  (update-in game
             (concat [:players (:current-player game)] path)
             f))

(defn update-current-player [game path f]
  (eventless-update-player-with
    (fn [player]
      (update-in player path f))
    game))

(defn update-player-with [f game]
  (let [player (current-player game)
        [updated-player data] (f player)
        updated-game (assoc-in game
                               [:players (:current-player game)]
                               updated-player)]
    [updated-game data]))

(defn associate-events-to-current-player [[game events success?]]
  [(assoc-in game
             [:players (:current-player game) :events]
             events)
   success?])
