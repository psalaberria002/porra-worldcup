(ns porra-worldcup.calculations
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [porra-worldcup.worldcup-api :as worldcup-api]))

(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader (io/file source))]
      (edn/read (java.io.PushbackReader. r)))

    (catch java.io.IOException e
      (printf "Couldn't open '%s': %s\n" source (.getMessage e)))
    (catch RuntimeException e
      (printf "Error parsing edn file '%s': %s\n" source (.getMessage e)))))

(defn get-porra-filenames []
  (let [porras-resource-folder "porras/"]
    (->> (clojure.java.io/file porras-resource-folder)
         io/file
         file-seq
         (map #(.getName %))
         (filter #(clojure.string/ends-with? % "edn"))
         (map #(str porras-resource-folder %)))))

(defn load-all-porras []
  (->> (map load-edn (get-porra-filenames))
       (map (fn [m] (update m :matches #(into (sorted-map) %))))))

(defn calculate-group-match-points [porra results]
  (let [result-matches (:matches results)]
    (->> (map (fn [[k bet]] (= bet (k result-matches))) (:matches porra))
         (remove false?)
         count
         (* 2))))

(defn calculate-group-standings-points [porra results]
  (let [group-standings-results (:group-standings results)]
    (->> (:group-standings porra)
         (map (fn [[gpos-kw team]]
                (= (gpos-kw group-standings-results) team)))
         (remove false?)
         count
         (* 5))))

(defn calculate-round-points [round-kw points porra results]
  (let [p (set (round-kw (:rounds porra)))
        res (set (round-kw (:rounds results)))]
    (->> (clojure.set/intersection p res)
         count
         (* points))))

(defn calculate-round-16-points [porra results]
  (calculate-round-points :round-16 5 porra results))

(defn calculate-quarter-final-points [porra results]
  (calculate-round-points :quarter 10 porra results))

(defn calculate-semi-final-points [porra results]
  (calculate-round-points :semi 15 porra results))

(defn calculate-final-points [porra results]
  (calculate-round-points :final 20 porra results))

(defn calculate-third-and-fourth-points [porra results]
  (calculate-round-points :third-and-fourth 15 porra results))

(defn calculate-winner-final-points [porra results]
  (if (= (:winner (:rounds porra))
         (:winner (:rounds results)))
    35
    0))

(defn calculate-pichichi-points [porra results]
  (if (= (:pichichi porra)
         (first (:pichichi results)))
    15
    0))

(defn calculate-points-for-porra [porra results]
  (+ (calculate-group-match-points porra results)
     (calculate-group-standings-points porra results)
     (calculate-round-16-points porra results)
     (calculate-quarter-final-points porra results)
     (calculate-semi-final-points porra results)
     (calculate-final-points porra results)
     (calculate-third-and-fourth-points porra results)
     (calculate-winner-final-points porra results)
     (calculate-pichichi-points porra results)
     ;; TODO: Get MVP
     ;(calculate-mvp-points porra results)
     )

  )

(defn calculate-all []
  (let [results (worldcup-api/generate-results-porra)]
    {:results results
     :porras  (->> (map (fn [p] (assoc p :points (calculate-points-for-porra p results))) (load-all-porras))
                   (sort-by :points)
                   reverse)}))
