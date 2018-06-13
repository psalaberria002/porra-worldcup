(ns porra-worldcup.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [porra-worldcup.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[porra-worldcup started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[porra-worldcup has shut down successfully]=-"))
   :middleware wrap-dev})
