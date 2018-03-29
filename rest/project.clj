(defproject rest "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.6.0"]
                 [hiccup "1.0.5"]
                 [ring-server "0.5.0"]
                 [clojurewerkz/cassaforte "3.0.0-alpha2-SNAPSHOT"]
                 [ring/ring-json "0.4.0"]
                 [clj-http "3.8.0"]]
  :plugins [[lein-ring "0.8.12"]]
  :ring {:handler rest.handler/app
         :init rest.handler/init
         :destroy rest.handler/destroy
         :nrepl {:start? true :port 3001}}
  :profiles
  {:uberjar {:aot :all}
   :production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false
     }}
   :dev
   {:dependencies [[ring-mock "0.1.5"] [ring/ring-devel "1.6.3"]]}})
