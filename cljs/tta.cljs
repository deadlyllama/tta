(ns tta.main
  (:use [jayq.core :only [$ css text html bind]]
        [jayq.util :only [log]])
  (:require [tta.views :as views])
  (:use-macros [jayq.macros :only [let-ajax ready]]))

(defn refresh-game []
  (let-ajax [game {:url "/api/game"}]
    (html ($ :#game) (views/render-game game))))

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
