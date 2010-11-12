(ns mississippi.web
  (:use compojure.core
        clojure.contrib.json
        ring.adapter.jetty
        ring.middleware.reload
        ring.util.response)
  (:require [mississippi.core :as miss]
            [compojure.route :as route]))

(defn- emit-json [x & [status]]
  {:headers {"Content-Type" "application/json"}
   :status (or status 200)
   :body (json-str x)})

(defresource Coffee {:coffee [(miss/member-of #{"latte" "drip"})]
                     :size [(miss/member-of #{"small" "medium" "large"})]
                     :quantity [miss/required (miss/in-range (range 1 4))]})

(defmacro POST+ [url resource symb & body]
  `(let [params# (gensym)]
     (POST ~url
           {params# :params}
           (let [~symb (new ~resource params#)]
             (do ~@body)))))

(macroexpand "/order" Coffee c (println "wah"))

(defroutes main-routes

  (GET "/orders/:id" [id]
       (emit-json {"coffee" "enjoy"}))

  (POST "/orders"
        {params :params}
        (let [order (Coffee. params)]
          (if-let [errors (errors order)]
            (emit-json errors 500)
            (redirect "/orders/1"))))

  ;; (VPOST "/orders"
  ;;        order-validations order
  ;;        (emit-json {:bitches order})))

(def app (-> #'main-routes
             (wrap-reload '[mississippi.web
                            mississippi.core])))

(defn start-server []
  (run-jetty #'app {:port 8080}))


