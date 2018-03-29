(ns rest.db
  (:require
   [clojurewerkz.cassaforte.client :as client]
;;   [clojurewerkz.cassaforte.cql    :refer :all]
   )
  )


(defonce session (client/connect ["127.0.0.1"]))

(defonce dummy
  (do
    (def search-item-prepared (atom nil))
    (def get-items-count-prepared (atom nil))
    (def get-item-prepared (atom nil))
    (def get-comments-prepared (atom nil))
    (def get-item-available-prepared (atom nil))
    ))

(defn get-session []
  session)

;;(def -prepared (atom nil))
;;(def -prepared (atom nil))
;;(def -prepared (atom nil))
;;(def -prepared (atom nil))

;; val addItemToBusketPrepared = dseSession.prepare("insert into atwaters_usa.cart(user_id, sku, updated, title, currency, price) " 
;;                                                  + " values(?, ?, toTimestamp(now()), ?, 'USD', ?) if not exists")


(defn init-db []
  (reset! get-items-count-prepared
          (client/prepare session "select items_count, updated from atwaters_usa.cart where user_id = ? limit 1;"))
  (reset! get-item-prepared
          (client/prepare session "select sku, title, price, urls, rating, rating_count from atwaters_inventory.inventory where sku = ? AND country = 'US';"))
  (reset! search-item-prepared
          (client/prepare session "select sku, base_sku, title, price, urls, rating, rating_count from atwaters_inventory.inventory where solr_query = ? limit 10"))
  (reset! get-comments-prepared
          (client/prepare session "select * from atwaters_inventory.comments where base_sku = ? limit 100"))
  (reset! get-item-available-prepared
          (client/prepare session "select * from atwaters_inventory.inv_counters where sku = ? and shop = ?"))

  )
