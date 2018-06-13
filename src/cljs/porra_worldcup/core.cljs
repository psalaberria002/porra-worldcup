(ns porra-worldcup.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]

            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]
            [porra-worldcup.ajax :refer [load-interceptors!]]
            [porra-worldcup.events]
            [secretary.core :as secretary])
  (:import goog.History))

(defn nav-link [uri title page]
  [:li.nav-item
   {:class (when (= page @(rf/subscribe [:page])) "active")}
   [:a.nav-link {:href uri} title]])

(defn navbar []
  [:nav.navbar.navbar-dark.bg-primary.navbar-expand-md
   {:role "navigation"}
   [:button.navbar-toggler.hidden-sm-up
    {:type "button"
     :data-toggle "collapse"
     :data-target "#collapsing-navbar"}
    [:span.navbar-toggler-icon]]
   [:a.navbar-brand {:href "#/"} "porra-worldcup"]
   [:div#collapsing-navbar.collapse.navbar-collapse
    [:ul.nav.navbar-nav.mr-auto
     [nav-link "#/" "Ranking" :ranking]
     [nav-link "#/matches" "Matches" :matches]
     [nav-link "#/results" "Results" :results]]]])

(defn matches-page []
  [:div.container
   [:div.wrapper
    [:table
     [:thead
      [:tr
       (for [c ["#" "Home" "Away" "Date" "City" "Result"]]
         [:th c])]]
     (when-let [ranking @(rf/subscribe [:matches])]
       [:tbody
        (doall (map-indexed (fn [i match]
                              [:tr
                               [:td.rank (:num match)]
                               [:td.team (:name (:team1 match))]
                               [:td.team (:name (:team2 match))]
                               [:td.date (str (:date match) "T" (:time match) " (" (:timezone match) ")")]
                               [:td.city (:city match)]
                               [:td.result (:result match)]])
                            ranking))])]]])

(defn results-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src "/img/warning_clojure.png"}]]]])

(defn ranking-page []
  [:div.container
   [:header
    [:h1 "Fifa World Cup Ranking"]]
   [:div.wrapper
    [:table
     [:thead
      [:tr
       (for [c ["Rank" "Name" "Points"]]
         [:th c])]]
     (when-let [ranking @(rf/subscribe [:ranking])]
       [:tbody
        (doall (map-indexed (fn [i [name points]]
                        [:tr
                         [:td.rank (+ 1 i)]
                         [:td.team name]
                         [:td.points points]])
                      ranking))])]]])

(def pages
  {:ranking #'ranking-page
   :matches #'matches-page
   :results #'results-page})

(defn page []
  [:div
   [navbar]
   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes

(secretary/set-config! :prefix "#")

(defn fetch-ranking! []
  (GET "/api/rankingpoints" {:handler #(rf/dispatch [:set-ranking %])}))

(defn fetch-matches! []
  (GET "/api/matches" {:handler #(rf/dispatch [:set-matches %])}))

(defn fetch-standings-with-results! []
  (GET "/api/standings" {:handler #(rf/dispatch [:set-standings %])}))

(secretary/defroute "/" []
  (fetch-ranking!)
  (rf/dispatch [:navigate :ranking]))

(secretary/defroute "/matches" []
  (fetch-matches!)
  (rf/dispatch [:navigate :matches]))

(secretary/defroute "/results" []
  (fetch-standings-with-results!)
  (rf/dispatch [:navigate :results]))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET "/docs" {:handler #(rf/dispatch [:set-docs %])}))

(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:navigate :ranking])
  (load-interceptors!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
