(ns gradesheet.model.quiz
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.result :refer [ok? has-error?]]))

(def conn (mg/connect))
(def db (mg/get-db conn "project")) ;; database name
(def document "quiz") ;; document

(defn add-quiz
  [quiz-result]
  (mc/insert-and-return db document quiz-result))

(defn get-quiz-questions
  [id]
  (let [qmap (mc/find-one-as-map db document {:id id} ["questions"])]
    (:questions qmap)))

(defn correct?
  [answer ans]
  (if (= answer ans) 1 0))

(defn get-score
  [id answers]
  (let [qmap (mc/find-one-as-map db document {:id id} ["answer"])]
    (reduce + (map correct? (:answer qmap) answers))))

(defn get-id
  [quiz]
  (:id quiz))

(defn get-quiz-ids
  []
  (let [qmap (mc/find-maps db document)]
    (into [] (map get-id qmap))))


