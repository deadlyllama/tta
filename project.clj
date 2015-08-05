(defproject tta "1.0.0-SNAPSHOT"
  :description "Through The Ages"
  :url ""
  :license {:name "Eclipse Public License v1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [ring-middleware-format "0.5.0"]
                 [environ "1.0.0"]
                 [jayq "2.5.4"]
                 [hiccups "0.3.0"]
                 [org.clojure/clojurescript "1.7.28"]
                 [org.clojure/algo.monads "0.1.5"]]
  :main tta.web
  :aot [tta.web]
  :min-lein-version "2.0.0"
  :plugins [[environ/environ.lein "0.3.1"]
            [lein-cljsbuild "1.0.6"]
            [lein-ring "0.9.6"]]
  :hooks [leiningen.cljsbuild]
  :uberjar-name "tta.jar"
  :profiles {:production {:env {:production true}}
             :dev {:dependencies [[midje "1.7.0"]]
                   :plugins [[lein-midje "3.1.3"]]}}
  :cljsbuild {:builds [{:source-paths ["cljs"]
                        :jar true
                        :compiler {:output-to "resources/public/js/app.js"
                                   :optimizations :whitespace
                                   :cache-analysis true
                                   :pretty-print true}}]}
  :ring {:handler tta.web/app})
