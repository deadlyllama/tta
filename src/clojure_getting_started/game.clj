(ns clojure-getting-started.game
  (:use [midje.sweet :only [fact facts]]))

(def initial-player-state
  {:buildings {:temple 1
               :farm 2
               :mine 2}})

(def game-state
  {:players [initial-player-state
             initial-player-state]})
