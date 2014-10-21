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

(defn produce-from [player building commodity]
  (let [amount (min (:supply player)
                    (get-in player [:buildings building]))]
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

(defn production-phase [game]
  "Updates the current player's board state according to the rules of the
  production phase."
  (let [player (current-player game)
        updated-player (-> player
                         (produce-from :farm :food)
                         (produce-from :mine :resources))]
    (assoc-in game [:players (:current-player game)] updated-player)))


(defn end-turn [game]
  (let [updated-game (production-phase game)]
    (if (last-players-turn? updated-game)
      (next-round updated-game)
      (next-player updated-game))))


