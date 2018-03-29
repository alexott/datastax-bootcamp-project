(ns rest.routes.search
  (:require [compojure.core :refer :all]
            [rest.views.layout :as layout]
            [hiccup.page :refer [html5 include-css]]
            [cheshire.core :as ch]
            [clojurewerkz.cassaforte.client :as client]
            [clj-http.client :as http]
;;            [clojurewerkz.cassaforte.cql    :refer :all]
            )
  (:use rest.utils
        rest.db
        )
  )

(defonce conn-manager (clj-http.conn-mgr/make-reusable-conn-manager {:timeout 5 :threads 20}))

(defn- do-search-api [term]
;;  (println "Search is called with" term)
  (let [solr-query {:fq "country:US" :q (str "title:\"" term "\"")}
        query-str (ch/generate-string solr-query)
        bound (client/bind @search-item-prepared [query-str])]
    (client/execute (get-session) bound)))

(defn- gen-search-page [term]
  (let [results (do-search-api term)]
    (html5
     [:head
      [:title "Search results"]
      (include-css "/css/screen.css")]
     [:body
      [:table
       [:thead nil
        [:tr nil [:th {:width "70%" :text-align "center"} "Item"]
         [:th {:width "20%" :text-align "center"} "Price"]
         [:th {:width "10%" :text-align "center"} "Rating"]]
        ]
       [:tbody nil
        (map (fn [r]
               [:tr nil (list [:td nil [:a {:href (str "/" (:sku r))} (:title r)]]
                              [:td nil (str (:price r))] [:td nil (str (:rating r))])])
              results)]
       ]
      ])
    ))

(defn- do-suggest [term]
  (let [res (http/get (str "http://localhost:8983"
                           "/solr/atwaters_inventory.inventory/suggest?suggest=true&suggest.dictionary=titleSuggester&suggest.q="
                           term "&suggest.cfq=US&wt=json")
                      {:connection-manager conn-manager :accept :json :as :json})
        ]
    (if (= (:status res) 200)
      (get-in res [:body :suggest :titleSuggester (keyword term) :suggestions])
      nil)))

(defroutes search-routes
  (GET "/search-api" [term callback] (wrap-callback callback (do-search term)))
  (GET "/search" [term] (gen-search-page term))
  (GET "/suggest" [term callback] (wrap-callback callback (do-suggest term)))
  )
