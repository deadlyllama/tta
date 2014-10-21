(ns tta.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]
            [tta.game :as game]))

(defn splash []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (slurp (io/resource "index.html"))})

(def current-game (atom game/sample-game-state))

(defroutes app
  (GET "/" []
       (splash))
  (GET "/api/game" []
       {:status 200
        :headers {"Content-Type" "application/edn"}
        :body (str @current-game)})
  (POST "/api/game/actions" [action]
        (swap! current-game game/pass)
        {:status 200
         :headers {"Content-Type" "application/edn"}
         :body (str ["jee"])})
  (GET "*" []
       (route/resources "/"))
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))