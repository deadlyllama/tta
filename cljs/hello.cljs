(ns app
  (:use [jayq.core :only [$ css text html bind]]
        [jayq.util :only [log]])
  (:use-macros [jayq.macros :only [let-ajax]])
  (:require-macros [hiccups.core :as hiccups])
  (:require [jayq.core :as jq]
            [hiccups.runtime :as hiccupsrt]))

(hiccups/defhtml show-player [player]
  [:div { :class "player" }
   [:h3 (:name player)]
   [:h4 "Buildings"]
   [:ul
    (for [[building building-count] (:buildings player)]
      [:li (str (name building) ": " building-count)])]])

(defn current-player [game]
  (get (:players game)
       (:current-player game)))

(hiccups/defhtml render-game [game]
  [:h2 (str "Current player: " (:name (current-player game)))]
  [:h2 (str "Round: " (:current-round game))]
  (map show-player
       (:players game)))

(defn refresh-game []
  (let-ajax [game {:url "/api/game"}]
    (html ($ :#game) (render-game game))))

(refresh-game)

(bind ($ :#pass) "click"
  (fn [event]
    (let-ajax [_ {:url "/api/game/actions"
                  :data "pass"
                  :type "POST"}]
      (refresh-game))))
