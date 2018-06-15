(ns porra-worldcup.worldcup-api
  (:require
    [clj-http.client :as client]
    [cheshire.core :as json]
    [clojure.core.memoize :as memoize]))

;; TODO: https://github.com/lsv/fifa-worldcup-2018
(def api-url "https://raw.githubusercontent.com/openfootball/world-cup.json/master/2018")

(defn get-fixtures-and-results []
  (-> (client/get (str api-url "/worldcup.json"))
      :body
      (json/decode true)))

(def get-fixtures-and-results-memoized
  (memoize/ttl get-fixtures-and-results :ttl/threshold 60))

(defn get-group-matches [fixtures-and-results]
  (->> fixtures-and-results
       (filter #(.startsWith (:name %) "Matchday"))
       (mapcat :matches)))

(defn game-finished? [game]
  (some (complement nil?) (vals (select-keys game [:score1 :score1i :score1et :score1p]))))

(defn get-result [game]
  (let [s1 (+ (or (:score1 game) 0)
              (or (:score1i game) 0)
              (or (:score1et game) 0)
              (or (:score1p game) 0))
        s2 (+ (or (:score2 game) 0)
              (or (:score2i game) 0)
              (or (:score2et game) 0)
              (or (:score2p game) 0))]
    [s1 s2]))

(defn get-game-winner [game]
  (when (game-finished? game)
    (let [[s1 s2] (get-result game)]
      (if (> s1 s2)
        (get-in game [:team1 :name])
        (get-in game [:team2 :name])))))

(defn add-match-result [game]
  (let [[s1 s2] (get-result game)]
    (assoc game :result (when (game-finished? game)
                           (if (> s1 s2)
                             "1"
                             (if (= s1 s2)
                               "X"
                               "2"))))))

(defn results->matchnumber-value [group-matches]
  (->> (map add-match-result group-matches)
       (map (fn [match]
              ((juxt (fn [m] (keyword (str (:num m)))) :result) match)))
       (into (sorted-map))))

(defn get-group-standings []
  (-> (client/get (str api-url "/worldcup.standings.json"))
      :body
      (json/decode true)))

(def get-group-standings-memoized
  (memoize/ttl get-group-standings :ttl/threshold 60))

(defn get-ranking [group]
  (let [group-number (->> group
                          :name
                          last
                          str
                          clojure.string/lower-case
                          keyword)]
    [group-number
     (->> group
          :standings
          (sort-by :pos)
          (map (fn [t] (get-in t [:team :name]))))]))

(defn get-first-two-teams-from-each-group [group-standings-sorted]
  (mapcat (fn [[k v]] ((juxt first second) v)) group-standings-sorted))

(defn get-winners-of-round [fixtures-and-results round-name]
  (let [winners (->> fixtures-and-results
                     (filter (fn [day] (= (:name day) round-name)))
                     first
                     :matches
                     (map get-game-winner))]
    winners))

(defn get-winner [fixtures-and-results]
  (first (get-winners-of-round fixtures-and-results "Final")))

(defn get-teams-in-quarter-finals [fixtures-and-results]
  (get-winners-of-round fixtures-and-results "Round of 16"))
(defn get-teams-in-semi-finals [fixtures-and-results]
  (get-winners-of-round fixtures-and-results "Quarter-finals"))
(defn get-teams-in-final [fixtures-and-results]
  (get-winners-of-round fixtures-and-results "Semi-finals"))
(defn get-teams-in-third-and-fourth-game [fixtures-and-results]
  (->> (clojure.set/difference (set (get-teams-in-semi-finals fixtures-and-results))
                                 (set (get-teams-in-final fixtures-and-results)))))

(defn get-all-matches [fixtures-and-results]
  (mapcat :matches fixtures-and-results))

(defn add-game-scores [game]
  (let [goals (:goals game)
        grouped (group-by :name goals)
        player-goals (map (fn [[name goals]] [name (count goals)]) grouped)]
    player-goals))

(defn get-max-scorers [fixtures-and-results]
  (let [all-matches (get-all-matches fixtures-and-results)]
    (->> (mapcat add-game-scores all-matches)
         (map #(apply array-map %))
         (apply merge-with +)
         (sort-by second)
         reverse
         (take 20))))

(defn group-matches-finished? [fixtures-and-results]
  (->> (filter #(= (:name %) "Round of 16") fixtures-and-results)
       first
       :matches
       not-empty))

(defn group-standings-sorted->gpos-team [group-standings-sorted]
  (into {}
        (mapcat (fn [[g teams]]
             (map-indexed (fn [pos team]
                            [(keyword (str (name g) (+ 1 pos))) team]) teams))
           group-standings-sorted)))

(defn generate-results-porra []
  (let [fixtures-and-results (:rounds (get-fixtures-and-results-memoized))
        group-matches (get-group-matches fixtures-and-results)
        group-standings (:groups (get-group-standings-memoized))
        group-standings-sorted (->> (map get-ranking group-standings)
                                    (into (sorted-map)))
        max-scorers (get-max-scorers fixtures-and-results)]
    {:matches         (results->matchnumber-value group-matches)
     :group-standings (when (group-matches-finished? fixtures-and-results)
                        (group-standings-sorted->gpos-team group-standings-sorted))
     :rounds          {:round-16         (when (group-matches-finished? fixtures-and-results)
                                           (get-first-two-teams-from-each-group group-standings-sorted))
                       :quarter          (get-teams-in-quarter-finals fixtures-and-results)
                       :semi             (get-teams-in-semi-finals fixtures-and-results)
                       :final            (get-teams-in-final fixtures-and-results)
                       :third-and-fourth (get-teams-in-third-and-fourth-game fixtures-and-results)
                       :winner           (get-winner fixtures-and-results)}
     :scorers         max-scorers
     :pichichi        (first max-scorers)}))

(defn format-match [m]
  (str (:num m) ". "
       (get-in m [:team1 :name]) " - " (get-in m [:team2 :name])
       " <" (:city m) " "
       (:date m) "T" (:time m) "(" (:timezone m) ")>"))

(defn get-match-number-and-teams []
  (map format-match (get-all-matches (:rounds (get-fixtures-and-results-memoized)))))

(defn all-matches []
  (map add-match-result (get-all-matches (:rounds (get-fixtures-and-results-memoized)))))

(defn get-teams []
  (-> (client/get (str api-url "/worldcup.teams.json"))
      :body
      (json/decode true)
      :teams))
