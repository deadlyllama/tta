(ns hello
  (:use [jayq.core :only [$ css text html]]
        [jayq.util :only [log]])
  (:use-macros [jayq.macros :only [let-ajax]])
  (:require-macros [hiccups.core :as hiccups])
  (:require [jayq.core :as jq]
            [hiccups.runtime :as hiccupsrt]))

(hiccups/defhtml show-player [player]
  [:div
   [:h3 "Player"]
   [:h4 "Buildings"]
   [:dl
    (->> (for [[building building-count] (:buildings player)]
           [[:dt building]
            [:dd building-count]])
         (apply concat))]])

(let-ajax [game {:url "/api/game"}]
  (html ($ :#game) (apply str
                          (map show-player
                               (:players game)))))
