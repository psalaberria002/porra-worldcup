(ns user
  (:require [porra-worldcup.config :refer [env]]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [mount.core :as mount]
            [porra-worldcup.figwheel :refer [start-fw stop-fw cljs]]
            [porra-worldcup.core :refer [start-app]]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(defn start []
  (mount/start-without #'porra-worldcup.core/repl-server))

(defn stop []
  (mount/stop-except #'porra-worldcup.core/repl-server))

(defn restart []
  (stop)
  (start))


