(ns mississippi.web
  (:use compojure.core
        clojure.contrib.json
        ring.adapter.jetty
        ring.middleware.reload
        ring.util.response
        mississippi.core)
  (:require [compojure.route :as route]
            [clojure.walk :as walk]))

(defn- emit-json [x & [status]]
  {:headers {"Content-Type" "application/json"}
   :status (or status 200)
   :body (json-str x)})

(defresource Coffee {:coffee   [(member-of #{"latte" "drip"})]
                     :size     [(member-of #{"small" "medium" "large"})]
                     :quantity [required (in-range (range 1 4))]})

(defroutes main-routes

  (GET "/orders/:id" [id]
       (emit-json {"coffee" "enjoy"}))

  (POST "/orders"
        {params :params}
        (let [order (Coffee. (walk/keywordize-keys params))]
          (if (valid? order)
            (redirect "/orders/1")
            (emit-json (errors order) 500)))))

(def app (-> #'main-routes
             (wrap-reload '[mississippi.web
                            mississippi.core])))

(defn start-server []
  (run-jetty #'app {:port 8080}))


