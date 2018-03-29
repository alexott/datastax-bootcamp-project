(ns rest.utils
  (:require [clojure.string :as s]
            [cheshire.core :as ch])
  )

(defn wrap-callback [callback data]
  (let [jsn (ch/generate-string data)]
    (if callback
      {:body (str callback "(" jsn ")")
       :headers {"Content-Type" "application/javascript; charset=UTF-8"}
       :status 200}
      {:body jsn
       :headers {"Content-Type" "application/json; charset=UTF-8"}
       :status 200}))
  )
