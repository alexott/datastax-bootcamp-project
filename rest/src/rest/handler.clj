(ns rest.handler
  (:require [compojure.core :refer [defroutes routes]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [hiccup.middleware :refer [wrap-base-url]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-response]]
            [rest.routes.home :refer [home-routes]]
            [rest.routes.search :refer [search-routes]]
            ))

(defn init []
  (println "rest is starting")
  (rest.db/init-db)
  )

(defn destroy []
  (println "rest is shutting down"))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> (routes home-routes search-routes app-routes)
      (handler/site)
      (wrap-base-url)
      ;;wrap-json-response
      ))
