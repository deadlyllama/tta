(ns app
  (:use [jayq.core :only [$ css text html bind]]
        [jayq.util :only [log]])
  (:use-macros [jayq.macros :only [let-ajax ready]])
  (:require-macros [hiccups.core :as hiccups])
  (:require [jayq.core :as jq]
            [hiccups.runtime :as hiccupsrt]))

(hiccups/defhtml show-player [player current-player]
  [:div {:class (if (= player current-player) "current-player" "player")}
   [:h3 (:name player)]
   [:h4 "Buildings"]
   [:ul
    (for [[building building-count] (:buildings player)]
      [:li (str (name building) ": " building-count)])]
   [:h4 "Commodities"]
   [:ul
    (for [[commodity commodity-count] (:commodities player)]
      [:li (str (name commodity) ": " commodity-count)])]
   [:h4 "Supply"]
   [:ul
    [:li (str "balance: " (:supply player))]]
   [:h4 "Worker pool"]
   [:ul
    [:li (str "balance: " (:worker-pool player))]]
   [:h4 "Population bank"]
   [:ul
    [:li (str "balance: " (:population-bank player))]]
   [:h4 "Events"]
   [:ul (for [event (:events player)]
          [:li event])]])

(defn current-player [game]
  (get (:players game)
       (:current-player game)))

(hiccups/defhtml render-game [game]
  [:h2 (str "Round: " (:current-round game))]
  (map #(show-player % (current-player game)) (:players game)))

(defn refresh-game []
  (let-ajax [game {:url "/api/game"}]
    (html ($ :#game) (render-game game))))

(ready
  (refresh-game)

  (bind ($ :#pass) "click"
    (fn [event]
      (let-ajax [_ {:url "/api/game/actions"
                    :data {:action "pass"}
                    :contentType "application/edn"
                    :type "POST"}]
        (refresh-game))))

  (bind ($ :#reset) "click"
    (fn [event]
      (let-ajax [_ {:url "/api/game/reset"
                    :data ""
                    :type "POST"}]
                (refresh-game))))

  (bind ($ :#increase-population) "click"
        (fn [event]
          (let-ajax [_ {:url "/api/game/actions"
                        :data {:action "increase-population"}
                        :contentType "application/edn"
                        :type "POST"}]
                    (refresh-game))))

  (bind ($ :#build-farm) "click"
        (fn [event]
          (let-ajax [_ {:url "/api/game/actions"
                        :data {:action "build-farm"}
                        :contentType "application/edn"
                        :type "POST"}]
            (refresh-game)))))

