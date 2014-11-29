(ns tta.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.format :refer [wrap-restful-format]]
            [environ.core :refer [env]]
            [tta.game :as game]
            [tta.actions :as actions]))

(defn splash []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (slurp (io/resource "index.html"))})

(def current-game (atom game/sample-game-state))

(def actions
  {"pass" game/end-turn
   "increase-population" (partial game/attempt-action
                                  actions/increase-population-action)
   "build-mine" (partial game/attempt-action actions/build-mine-action)
   "build-farm" (partial game/attempt-action actions/build-farm-action)
   "build-temple" (partial game/attempt-action actions/build-temple-action)
   "build-lab" (partial game/attempt-action actions/build-lab-action)
   })

(defroutes tta-routes
  (GET "/" []
       (splash))
  (GET "/api/game" []
       {:status 200
        :body @current-game})
  (POST "/api/game/actions" [action]
        (swap! current-game (get actions action))
        {:status 200
         :body ["jee"]})
  (POST "/api/game/reset" []
        (reset! current-game game/sample-game-state)
        {:status 200
         :body ["jee"]})
  (GET "*" []
       (route/resources "/"))
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(def app
  (-> tta-routes
      (wrap-restful-format :formats [:edn])))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty #'app {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
