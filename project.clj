(defproject darktower "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.6.1"

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"
                  :exclusions [org.clojure/tools.reader]]
                 [org.clojure/core.async "0.3.465"
                  :exclusions [org.clojure/tools.reader]]
                 [com.taoensso/encore "2.93.0"
                  :exclusions [org.clojure/tools.reader]]
                 [com.taoensso/sente "1.12.0"
                  :exclusions [org.clojure/tools.reader]]
                 [com.taoensso/timbre "4.10.0"]
                 [environ "1.1.0"]
                 [http-kit "2.2.0"]
                 [compojure "1.6.0"]
                 [ring "1.6.3"
                  :exclusions [ring/ring-core commons-fileupload]]
                 [ring/ring-defaults "0.3.1"]
                 [ring-cors "0.1.11"]
                 [reagent "0.7.0"]
                 [prismatic/schema "1.1.7"]]

  :plugins [[lein-figwheel "0.5.2"]
            [lein-count "1.0.8"]
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
