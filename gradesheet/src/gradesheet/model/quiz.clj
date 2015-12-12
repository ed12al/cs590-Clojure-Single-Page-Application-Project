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
    (reduce + (map correct? (:answers qmap) answers))))

(add-quiz  { :questions [ { :question "Which state in the US is the largest?"
                            :choices ["California" "Alaska" "Texas"]
                          }
                          { :question "What is the capital of the US?"
                            :choices ["Washington D.C." "Los Angeles" "New York"]
                          }
                          { :question "Which state in the US has the most population?"
                            :choices ["California" "Texas" "New York"]
                          }
                        ]
             :id 24
             :answer ["Alaska" "Washington D.C." "California"]
            }

)



(add-quiz { :questions [ { :question "What causes night and day?"
                            :choices ["The earth spins on its axis" "The earth moves around the sun" "Clouds block out the sunlight"]
                          }
                          { :question "Grand Central Terminal, Park Avenue, New York is the world's"
                            :choices ["largest railway station" "highest railway station" "longest railway station"]
                          }
                          { :question "Entomology is the science that studies"
                            :choices ["Behavior of human beings" "Insects" "The origin and history of technical and scientific terms"]
                          }
                        ]
             :id 25
             :answer ["The earth spins on its axis" "largest railway station" "Insects"]
            }
)

(add-quiz { :questions [ { :question "Eritrea, which became the 182nd member of the UN in 1993, is in the continent of"
                            :choices ["Asia" "Africa" "Europe"]
                          }
                          { :question "Garampani sanctuary is located at"
                            :choices ["Junagarh, Gujarat" "Diphu, Assam" "Kohima, Nagaland"]
                          }
                          { :question "For which of the following disciplines is Nobel Prize awarded?"
                            :choices ["Physics and Chemistry" "Physiology or Medicine" "All of the above"]
                          }
                        ]
             :id 26
             :answer ["Africa" "Diphu, Assam" "All of the above"]
            }
)
