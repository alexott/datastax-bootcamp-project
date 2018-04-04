(ns rest.routes.home
  (:require [compojure.core :refer :all]
            [rest.views.layout :as layout]
            [hiccup.page :refer [html5 include-css]]
            [clojurewerkz.cassaforte.client :as client]
            )
  (:use rest.db)
  )

(defn- get-main-page-items []
  (let [random (java.util.Random.)
        ids (repeatedly 20 #(java.util.UUID/fromString (format "f5c03dd1-2e78-11e8-8d2e-%012d" (.nextInt random 1000000))))
        futures (mapv (fn [id]
                        (client/async (client/execute (get-session) (client/bind @get-item-prepared [id]))))
                      ids)]
    (filterv seq (mapv (comp first deref) futures))))

(defn- main-page []
  (let [results (get-main-page-items)]
    (html5
     [:head
      [:title "Atwater's"]
      (include-css "/css/screen.css")
      [:script {:src "/js/suggest.js"}]
      ]

     [:body
      [:h1 "Welcome to Atwater's"]
      [:form {:action "/search"}
       [:input {:type "text" :name "term" :size 50}]
       [:input {:type "submit" :value "Search"}]
       ]

      [:hr]
      [:p "Promoted items"]
      [:br]
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
    )


  )

(defn- get-page [id]
  
  )


(defroutes home-routes
  (GET "/" [] (main-page))
  (GET "/:id" [id] (get-page id))

  )
