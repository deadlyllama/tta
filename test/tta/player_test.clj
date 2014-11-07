(ns tta.player-test
  (:use [midje.sweet :only [facts fact => contains]]
        ;no bullshit, only  facts
        tta.player
        [tta.game :only [sample-game-state]]))

(fact
  (:name (create-player "Juhana")) => "Juhana")

(fact "eventless-update-player-with"
  (->> sample-game-state
       (eventless-update-player-with (fn [player]
                                       (assoc player :population-bank 5)))
       current-player
       :population-bank)
  => 5)

