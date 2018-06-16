(ns porra-worldcup.fifa-worldcup-2018
  (:require
    [clj-http.client :as client]
    [cheshire.core :as json]
    [clojure.core.memoize :as memoize]))

(def api-url "http://worldcup.sfg.io")

(defn get-matches []
  (-> (client/get (str api-url "/matches"))
      :body
      (json/decode true)))

(def get-matches-memoized
  (memoize/ttl get-matches :ttl/threshold 60000))

(defn match-results [matches]
  (into {} (map (fn [m]
          (let [winner (:winner m)
                home_team (:country (:home_team m))
                away_team (:country (:away_team m))
                res (when winner
                      (condp = winner
                        "Draw" "X"
                        home_team "1"
                        away_team "2"))]
            [[home_team away_team] res]))
        matches)))

(defn get-group-results []
  (-> (client/get (str api-url "/teams/group_results"))
      :body
      (json/decode true)))

(def get-group-results-memoized
  (memoize/ttl get-group-results :ttl/threshold 60000))

(defn get-group-matches [matches]
  (->> matches
       (take 48)))

(defn get-winners-of-round [matches first-game-of-round last-game-of-round]
  (let [winners (->> (subvec (vec matches) first-game-of-round (+ 1 last-game-of-round))
                     (map :winner))]
    (set winners)))

(defn get-winner [matches]
  (:winner (last matches)))

(defn get-teams-in-quarter-finals [matches]
  (get-winners-of-round matches 48 55))
(defn get-teams-in-semi-finals [matches]
  (get-winners-of-round matches 56 59))
(defn get-teams-in-final [matches]
  (get-winners-of-round matches 60 61))
(defn get-teams-in-third-and-fourth-game [matches]
  (->> (clojure.set/difference (set (get-teams-in-semi-finals matches))
                               (set (get-teams-in-final matches)))))

(defn group-matches-finished? [group-matches]
  (when (:winner (last group-matches))
    true))

(defn get-group-standings [group-results]
  (into (sorted-map) (mapcat (fn [g]
                               (let [teams-sorted (get-in g [:group :teams])
                                     g-letter (clojure.string/lower-case (get-in g [:group :letter]))]
                                 (map-indexed (fn [i t] [(keyword (str g-letter i)) (:country (:team t))]) teams-sorted))) group-results)))

(defn get-first-two-from-each-group [group-results]
  (set (mapcat (fn [g]
                 (let [teams-sorted (get-in g [:group :teams])]
                   (->> ((juxt first second) teams-sorted)
                        (map (fn [t] (:country (:team t))))))) group-results)))

(defn generate-results-porra []
  (let [matches (get-matches-memoized)
        group-matches (get-group-matches matches)
        group-results (get-group-results-memoized)]
    {:matches         (match-results group-matches)
     :group-standings (when (group-matches-finished? group-matches)
                        (get-group-standings group-results))
     :rounds          {:round-16         (when (group-matches-finished? group-matches)
                                           (get-first-two-from-each-group group-results))
                       :quarter          (get-teams-in-quarter-finals matches)
                       :semi             (get-teams-in-semi-finals matches)
                       :final            (get-teams-in-final matches)
                       :third-and-fourth (get-teams-in-third-and-fourth-game matches)
                       :winner           (get-winner matches)}
     :pichichi        ""}))


