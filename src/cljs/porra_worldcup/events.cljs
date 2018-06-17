(ns porra-worldcup.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-sub register-handler]]
            [ajax.core :refer [GET POST]]))

;;dispatchers

(reg-event-db
  :navigate
  (fn [db [_ page]]
    (assoc db :page page)))

(reg-event-db
  :set-docs
  (fn [db [_ docs]]
    (assoc db :docs docs)))

(reg-event-db
  :set-ranking
  (fn [db [_ r]]
    (assoc db :ranking r)))

(reg-event-db
  :set-standings
  (fn [db [_ r]]
    (assoc db :standings r)))

(reg-event-db
  :set-matches
  (fn [db [_ r]]
    (assoc db :matches r)))

(reg-event-db
  :set-teams
  (fn [db [_ r]]
    (assoc db :teams r)))

(reg-event-db
  :hide-porra-saved
  (fn [db [_ r]]
    (assoc db :porra-saved false)))

(reg-event-db
  :show-porra-saved
  (fn [db [_ r]]
    (assoc db :porra-saved true)))

(reg-event-db
  :select-porra
  (fn [db [_ r]]
    (assoc db :selected-porra r)))



;;subscriptions

(reg-sub
  :page
  (fn [db _]
    (:page db)))

(reg-sub
  :docs
  (fn [db _]
    (:docs db)))

(reg-sub
  :ranking
  (fn [db _]
    (:ranking db)))

(reg-sub
  :standings
  (fn [db _]
    (:standings db)))

(reg-sub
  :matches
  (fn [db _]
    (:matches db)))

(reg-sub
  :teams
  (fn [db _]
    (:teams db)))

(reg-sub
  :porra-saved
  (fn [db _]
    (:porra-saved db)))

(reg-sub
  :selected-porra
  (fn [db _]
    (:selected-porra db)))


