(ns gradesheet.model.quiz
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.result :refer [ok? has-error?]]))

(def conn (mg/connect))
(def db (mg/get-db conn "hw3")) ;; database name
(def document "quiz") ;; document

(defn add-quiz
  [quiz-result]
  (mc/insert-and-return db document quiz-result))

(defn get-quiz-question-by-id
  [id]
  (let [qmap (mc/find-one-as-map db document {:id id} ["id" "question" "choices"])
        qid (:id qmap)
        qq (:question qmap)
        qc (:choices qmap)]
    {:id qid :question qq :choices qc}))

(defn get-quiz-answer-by-id
  [id]
  (mc/find-one-as-map db document {:id id} ["answer"]))

(defn correct?
  [id answer]
  (if (= (:answer (get-quiz-answer-by-id id)) answer)
    true
    false))


;;(add-quiz {:id 12
;;           :question "What is the mascot of CSULA?"
;;           :answer "Eddie the Golden Eagle"
;;           :choices {:c1 "Eddy the Golden Bear"
;;                     :c2 "Eddy the Golden Eagle"
;;                     :c3 "Eddie the Golden Bear"
;;                     :c4 "Eddie the Golden Eagle"}})


;;(correct? 1 "Washington DC")

;;(:answer (get-quiz-answer-by-id 1))
