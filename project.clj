(defproject darktower "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.6.1"
  
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.34"]
                 [org.clojure/core.async "0.2.374"
                  :exclusions [org.clojure/tools.reader]]
                 [com.taoensso/sente "1.8.1"]
                 [com.taoensso/timbre "4.3.1"]
                 [environ "1.0.2"]
                 [http-kit "2.1.19"]
                 [compojure "1.5.0"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.2.0"]
                 [ring-cors "0.1.7"]
                 [reagent "0.6.0-rc"]
                 [prismatic/schema "1.1.2"]]
  
  :plugins [[lein-figwheel "0.5.2"]
            [lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]]

  :jvm-opts ["-XX:-OmitStackTraceInFastThrow"]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :main darktower.server.main

  :uberjar-name "darktower-standalone.jar"

  :profiles {:dev {:env {:dev? "true"}
                   :cljsbuild {:builds
                               [{:id "dev"
                                 :source-paths ["src" "dev"]
                                 :figwheel {}
                                 :compiler {:main darktower.main
                                            :asset-path "js/compiled/out"
                                            :output-to "resources/public/js/compiled/darktower.js"
                                            :output-dir "resources/public/js/compiled/out"
                                            :source-map-timestamp true}}]}}
             :uberjar {:hooks [leiningen.cljsbuild]
                       :aot :all
                       :cljsbuild {:builds
                                   [{:id "min"
                                     :source-paths ["src" "prod"]
                                     :compiler {:main darktower.main
                                                :output-to "resources/public/js/compiled/darktower.js"
                                                :optimizations :advanced
                                                :pretty-print false}}]}}}

  :figwheel {:css-dirs ["resources/public/css"]})
