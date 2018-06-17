(ns porra-worldcup.core
  (:use-macros [cljs.core.async.macros :only [go]])
  (:require [cljs.core.async :refer [<! timeout]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.selection-list :refer [selection-list]]
            [reagent-forms.core :refer [bind-fields init-field value-of]]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]
            [porra-worldcup.ajax :refer [load-interceptors!]]
            [porra-worldcup.events]
            [secretary.core :as secretary]
            [json-html.core :as edn]
            [cljs.pprint :refer [pprint]])
  (:import goog.History))

(defn nav-link [uri title page]
  [:li.nav-item
   {:class (when (= page @(rf/subscribe [:page])) "active")}
   [:a.nav-link {:href uri} title]])

(defn navbar []
  [:nav.navbar.navbar-dark.bg-primary.navbar-expand-md
   {:role "navigation"}
   [:button.navbar-toggler.hidden-sm-up
    {:type        "button"
     :data-toggle "collapse"
     :data-target "#collapsing-navbar"}
    [:span.navbar-toggler-icon]]
   [:a.navbar-brand {:href "#/"} "porra-worldcup"]
   [:div#collapsing-navbar.collapse.navbar-collapse
    [:ul.nav.navbar-nav.mr-auto
     [nav-link "#/" "Ranking" :ranking]
     [nav-link "#/matches" "Matches" :matches]
     [nav-link "#/form" "Form" :form]]]])

(defn matches-page []
  [:div.container
   [:div.wrapper
    [:table
     [:thead
      [:tr
       (for [c ["#" "Home" "Away" "Date" "City" "Result"]]
         [:th c])]]
     (when-let [matches @(rf/subscribe [:matches])]
       [:tbody
        (doall (map (fn [match]
                      [:tr
                       [:td.rank (:num match)]
                       [:td.team (:name (:team1 match))]
                       [:td.team (:name (:team2 match))]
                       [:td.date (str (:date match) "T" (:time match) " (" (:timezone match) ")")]
                       [:td.city (:city match)]
                       [:td.result (:result match)]])
                    matches))])]]])

(defn row [label input]
  [:div.row
   [:div.col-md-2 [:label label]]
   [:div.col-md-5 input]])

(defn form [matches teams]
  [:div.container
   [:input.form-control {:field       :text :id :name
                         :placeholder "Name here!!!"
                         :validator   (fn [doc]
                                        (when (-> doc :name empty?)
                                          ["error"]))}]
   (for [match matches]
     (row (str (:name (:team1 match)) " - " (:name (:team2 match)))
          [:select.form-control {:field :list :id (keyword (str "matches." (:num match)))}
           [:option {:key "1"} "1"]
           [:option {:key "X"} "X"]
           [:option {:key "2"} "2"]]))
   (let [groups (group-by :group matches)]
     (for [[g gmatches] groups]
       (let [gteams (->> (map :team1 gmatches)
                         set)]
         [:div
          [:h2 g]
          (for [i (range 4)]
            [:div
             (let [gpos (clojure.string/lower-case (str (first (reverse g)) (+ i 1)))]
               (row (clojure.string/upper-case gpos)
                    [:select.form-control {:field :list :id (keyword (str "group-standings." gpos))}
                     (for [t (sort-by :name gteams)]
                       [:option {:key (:name t)} (:name t)])]))
             ])])))
   (for [[round-kw amount] [[:round-16 16] [:quarter 8] [:semi 4] [:final 2] [:third-and-fourth 2]]]
     [:div
      [:h2 (str round-kw " (" amount " teams)")]
      [:ul.list-group {:field :multi-select :id (keyword (str "rounds." (name round-kw)))}
       (for [team (sort-by :name teams)]
         [:li.list-group-item {:key (:name team)} (:name team)])]])
   [:div
    (row "Winner"
         [:select.form-control {:field :list :id :rounds.winner}
          (for [team (sort-by :name teams)]
            [:option {:key (:name team)} (:name team)])])]
   (row "Pichichi"
        [:input.form-control {:field       :text :id :pichichi
                              :placeholder "Pichichi"}])
   (row "MVP"
        [:input.form-control {:field       :text :id :mvp
                              :placeholder "MVP"}])
   ])

(defn form-page []
  (let [doc (r/atom {:name            ""
                     :matches         {}
                     :group-standings {}
                     :rounds          {:round-16         []
                                       :quarter          []
                                       :semi             []
                                       :final            []
                                       :third-and-fourth []
                                       :winner           ""}
                     :pichichi        ""
                     :mvp             ""}
                    )]
    (fn []
      (let [matches @(rf/subscribe [:matches])
            teams @(rf/subscribe [:teams])
            saved? @(rf/subscribe [:porra-saved])]
        [:div
         [bind-fields (form matches teams) doc]
         #_[:label (str @doc)]
         [:button
          {:on-click #(POST "/api/save-porra"
                            {:params        (let [to-set (fn [v] (set v))]
                                              (-> @doc
                                                  (update-in [:rounds :round-16] to-set)
                                                  (update-in [:rounds :quarter] to-set)
                                                  (update-in [:rounds :semi] to-set)
                                                  (update-in [:rounds :final] to-set)
                                                  (update-in [:rounds :third-and-fourth] to-set)))
                             :handler       (fn [r]         ;;TODO: Show message
                                              (rf/dispatch [:show-porra-saved true])
                                              (go
                                                (<! (timeout 3000))
                                                (rf/dispatch [:hide-porra-saved true])))
                             :error-handler (fn [r]         ;;TODO: Show error message
                                              )})}
          (if (not saved?)
            "Save and send"
            "Saved successfully ;)")]]))))

(defn ranking-page []
  (let [standings @(rf/subscribe [:standings])
        selected-porra @(rf/subscribe [:selected-porra])]
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
                               [:tr {:on-click #(rf/dispatch [:select-porra name])}
                                [:td.rank (+ 1 i)]
                                [:td.team name]
                                [:td.points points]])
                             ranking))])]]
    [:div
     (when selected-porra
       [:div
        [:h3 selected-porra]
        [:pre [:code (with-out-str (pprint (first (filter #(= (:name %) selected-porra) (:porras standings)))))]]])
     (when standings
       [:div
        [:h3 "Results"]
        [:pre [:code (with-out-str (pprint (:results standings)))]]])]]))

(def pages
  {:ranking #'ranking-page
   :matches #'matches-page
   :form    #'form-page})

(defn page []
  [:div
   [navbar]
   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes

(secretary/set-config! :prefix "#")

(defn fetch-ranking! []
  (GET "/api/rankingpoints" {:handler #(rf/dispatch [:set-ranking %])}))

(defn fetch-standings! []
  (GET "/api/standings" {:handler #(rf/dispatch [:set-standings %])}))

(defn fetch-matches! []
  (GET "/api/matches" {:handler #(rf/dispatch [:set-matches %])}))

(defn fetch-teams []
  (GET "/api/teams" {:handler #(rf/dispatch [:set-teams %])}))

(secretary/defroute "/" []
  (fetch-ranking!)
  (rf/dispatch [:navigate :ranking]))

(secretary/defroute "/matches" []
  (fetch-matches!)
  (rf/dispatch [:navigate :matches]))

(secretary/defroute "/form" []
  (fetch-teams)
  (fetch-matches!)
  (rf/dispatch [:navigate :form]))

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
  (fetch-teams)
  (fetch-matches!)
  (fetch-standings!)
  (hook-browser-navigation!)
  (mount-components))
