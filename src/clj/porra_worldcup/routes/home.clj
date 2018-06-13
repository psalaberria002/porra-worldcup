(ns porra-worldcup.routes.home
  (:require [porra-worldcup.layout :as layout]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [porra-worldcup.calculations :as calcs]
            [porra-worldcup.worldcup-api :as worldcup-api]
            [clj-time.local :as l]
            [clj-time.coerce :as c]))

(defn home-page []
  (layout/render "home.html"))

(defn write-dataset-edn! [out-file raw-dataset-map]
  (with-open [w (clojure.java.io/writer out-file)]
    (binding [*out* w]
      (clojure.pprint/write raw-dataset-map))))

(defroutes home-routes
  (GET "/" []
       (home-page))
  (GET "/docs" []
       (-> (response/ok (-> "docs/docs.md" io/resource slurp))
           (response/header "Content-Type" "text/plain; charset=utf-8"))))

(defapi service-routes
        {:swagger {:ui "/swagger-ui"
                   :spec "/swagger.json"
                   :data {:info {:version "1.0.0"
                                 :title "Sample API"
                                 :description "Sample Services"}}}}

        (context "/api" []
                 :tags ["porra"]

                 (GET "/standings" []
                   :summary "Return ranking"
                   (response/ok (calcs/calculate-all)))

                 (POST "/save-porra" []
                       :body [b s/Any]
                       (response/ok (write-dataset-edn! (str (:name b)
                                                             "-"
                                                             (c/to-long (l/local-now)))
                                                        b)))

                 (GET "/teams" []
                      (response/ok (worldcup-api/get-teams)))

                 (GET "/rankingpoints" []
                   :summary "Return ranking with points"
                   (response/ok (map (partial (juxt :name :points)) (:porras (porra-worldcup.calculations/calculate-all)))))

                 (GET "/matches" []
                      (response/ok (worldcup-api/all-matches)))

                 (GET "/matches-formatted" []
                   (response/ok (worldcup-api/get-match-number-and-teams)))))
