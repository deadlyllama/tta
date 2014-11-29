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
   [:h4 (str "Culture: " (:culture player))]
   [:h4 "Buildings"]
   [:ul
    (for [[building building-count] (:buildings player)]
      [:li (str (name building) ": " building-count)])]
   [:h4 "Commodities"]
   [:ul
    (for [[commodity commodity-count] (:commodities player)]
      [:li (str (name commodity) ": " commodity-count)])]
   [:h4 (str "Supply: " (:supply player))]
   [:h4 (str "Worker pool: " (:worker-pool player))]
   [:h4 (str "Population bank: " (:population-bank player))]
   [:h4 (str "Civil actions: "
             (get-in player [:civil-actions :remaining])
             "/"
             (get-in player [:civil-actions :total]))]
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

(defn bind-action [button action]
  (bind button "click"
        (fn [event]
          (let-ajax [_ {:url "/api/game/actions"
                        :data {:action action}
                        :contentType "application/edn"
                        :type "POST"}]
            (refresh-game)))))

(ready
  (refresh-game)

  (bind-action ($ :#pass) "pass")
  (bind-action ($ :#increase-population) "increase-population")
  (bind-action ($ :#build-farm) "build-farm")
  (bind-action ($ :#build-mine) "build-mine")
  (bind-action ($ :#build-temple) "build-temple")

  (bind ($ :#reset) "click"
    (fn [event]
      (let-ajax [_ {:url "/api/game/reset"
                    :data ""
                    :type "POST"}]
                (refresh-game)))))
