(ns tta.game
  (:use [midje.sweet :only [fact facts =>]]))

(def initial-player-state
  {:buildings {:temple 1
               :farm 2
               :mine 2}
   :consumables {:food 0}})

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

(defn pass [game]
  (if (last-players-turn? game)
    (next-round game)
    (next-player game)))

(defn current-player [game]
  (get (:players game)
       (:current-player game)))

(defn production-phase [game]
  "Updates the current player's board state
   according to the rules of the production phase."
  (let [player (current-player game)
        updated-player
          (update-in player [:consumables :food]
                     (fn [food]
                       (+ food (get-in player [:buildings :farm]))))]
    (assoc-in game [:players (:current-player game)] updated-player)))

(fact
  (get-in (production-phase sample-game-state)
          [:players 0 :consumables :food]) => 2
  (get-in (production-phase (assoc sample-game-state :current-player 1))
          [:players 1 :consumables :food]) => 2)

(let [game sample-game-state
      game2 (assoc game :current-player 2)]
  (fact
    (pass game) => (assoc game :current-player 1)
    (pass game2) => (assoc game2 :current-player 0
                                 :current-round 2)))
