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
   [:h4 (str "Science: " (:science player))]
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

(defn show-urban-building [& {:keys [building-id building-type building-name
                                     amount-built amount-remaining
                                     price building-info]
                              :or {amount-remaining 0}}]
   [:div {:id building-id, :class "urban building"}
    [:div {:class "header"}
     [:span {:class "era"} "A"]
     [:span {:class "building-type"} building-type]]
    [:div {:class "building-area"}
     [:h4 building-name]

     [:div {:class "building-meter"}
      [:table {:cellspacing "0"}
       (concat (repeat amount-built
                       [:td {:class "meter-block"}
                        [:div {:class "meter-bar population-filled"}
                         "&nbsp;"]])
               (repeat amount-remaining
                       [:td {:class "meter-block"}
                        [:div {:class "meter-bar population-empty"}
                         "&nbsp;"]]))]]

     [:p price]]
    [:div {:class "building-info"}
     building-info]])

(defn show-production-building [& {:keys [building-id building-type
                                          building-name amount-built
                                          building-info price
                                          commodity-count]}]
  [:div {:id building-id, :class "production building"}
   [:div {:class "header"}
    [:span {:class "era"} "A"]
    [:span {:class "building-type"} building-type]]
   [:div {:class "building-area"}
    [:h4 building-name]
    [:div {:class "building-meter"}
     [:table {:cellspacing "0"}
      (repeat amount-built
              [:td {:class "meter-block"}
               [:div {:class "meter-bar population-filled"}
                "&nbsp;"]])]]
    [:div {:class "building-meter"}
     [:table {:cellspacing "0"}
      (repeat commodity-count
              [:td {:class "meter-block"}
               [:div {:class "meter-bar supply-filled"}
                "&nbsp;"]])]]
    [:p price]]
   [:div {:class "building-info"}
    building-info]])

(defn show-player-board [player]
  [:div {:id "newgame"}
   (let [lab-count (get-in player [:buildings :lab])
         labs-remaining (- 2 lab-count)]
     (show-urban-building :building-id "lab"
                          :building-type "lab"
                          :building-name "Philosophy"
                          :amount-built lab-count
                          :amount-remaining labs-remaining
                          :price "3 ROCKS"
                          :building-info "+1 SCIENCE"))

   "&nbsp;"

   (let [temple-count (get-in player [:buildings :temple])
         temples-remaining (- 2 temple-count)]
     (show-urban-building :building-id "temple"
                          :building-type "temple"
                          :building-name "Religion"
                          :amount-built temple-count
                          :amount-remaining temples-remaining
                          :price "3 ROCKS"
                          :building-info "+1 CÜLTÜÜRI, +1 🐱"))

   "&nbsp;"

   (let [farm-count (get-in player [:buildings :farm])
         food-count (get-in player [:commodities :food])]
     (show-production-building :building-id "farm"
                               :building-type "farm"
                               :building-name "Agriculture"
                               :amount-built farm-count
                               :price "2 ROCKS"
                               :building-info "1 FÖÖD"
                               :commodity-count food-count))

   "&nbsp;"

   (let [mine-count (get-in player [:buildings :mine])
         resource-count (get-in player [:commodities :resources])]
     (show-production-building :building-id "mine"
                               :building-type "mine"
                               :building-name "Bronze"
                               :amount-built mine-count
                               :price "2 ROCKS"
                               :building-info "1 CIVEN"
                               :commodity-count resource-count))])

(defn current-player [game]
  (get (:players game)
       (:current-player game)))

(hiccups/defhtml render-game [game]
  (show-player-board (current-player game))
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
  (bind-action ($ :#build-lab) "build-lab")

  (bind ($ :#reset) "click"
    (fn [event]
      (let-ajax [_ {:url "/api/game/reset"
                    :data ""
                    :type "POST"}]
                (refresh-game)))))
