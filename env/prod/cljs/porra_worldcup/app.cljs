(ns porra-worldcup.app
  (:require [porra-worldcup.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
