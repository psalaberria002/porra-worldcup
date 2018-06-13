(ns porra-worldcup.handler
  (:require [porra-worldcup.layout :refer [error-page]]
            [porra-worldcup.routes.home :refer [home-routes service-routes]]
            [compojure.core :refer [routes wrap-routes]]
            [compojure.route :as route]
            [porra-worldcup.env :refer [defaults]]
            [mount.core :as mount]
            [porra-worldcup.middleware :as middleware]))

(mount/defstate init-app
                :start ((or (:init defaults) identity))
                :stop ((or (:stop defaults) identity)))

(mount/defstate app
  :start
  (middleware/wrap-base
    (routes
      #'service-routes
      (-> #'home-routes
          (wrap-routes middleware/wrap-csrf)
          (wrap-routes middleware/wrap-formats))
      (route/not-found
        (:body
          (error-page {:status 404
                       :title  "page not found"}))))))

