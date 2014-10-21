(ns tta.game)

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
    (-> player
      (update-in [:commodities commodity] #(+ % amount))
      (update-in [:supply] #(- % amount)))))

(defn corruption [player]
  (let [supply (:supply player)]
    (cond
      (= supply 0) 6
      (< supply 5) 4
      (< supply 9) 2
      :else        0)))

(defn unpaid-corruption [player]
  (let [resources (get-in player [:commodities :resources])
        corruption (corruption player)]
    (if (> corruption resources)
      (- corruption resources)
      0)))

(defn pay-corruption [player]
  (let [corruption (min (corruption player) (get-in player [:commodities :resources]))]
    (-> player
      (update-in [:commodities :resources] #(- % corruption))
      (update-in [:supply] #(+ % corruption)))))

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

(defn update-current-player [game updated-player]
  )

(defn update-player-with [f game]
  (let [player (current-player game)
        updated-player (f player)
        updated-game (assoc-in game [:players (:current-player game)] updated-player)]
    updated-game))

(defn produce-food [game]
  [(update-player-with #(produce-from :farm :food %) game)
   [(str "Produced "
        (production-amount :farm :food (current-player game))
        " food")]])

(defn produce-resources [game]
  [(update-player-with #(produce-from :mine :resources %) game)
   [(str "Produced "
        (production-amount :mine :resources (current-player game))
        " resources")]])

(defn production-phase [game]
  (let [[with-food events] (produce-food game)
        [with-resources events2] (produce-resources with-food)]
    [with-resources (concat events events2)]))

(defn end-turn [game]
  (let [[updated-game events] (production-phase game)
        with-events (assoc-in updated-game
                              [:players (:current-player game) :events]
                              events)]
    (if (last-players-turn? with-events)
      (next-round with-events)
      (next-player with-events))))

