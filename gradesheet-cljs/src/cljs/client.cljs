(ns gradesheet-cljs.client
  (:require [goog.net.XhrIo :as xhr]
            [domina :as d]
            [domina.events :as events]))

(def username-id "usename")
(def password-id "pass")
(def usernameResult "usernameResult")
(def passwordResult "passwordResult")
(def result-id "result")
(def button-id "eval-button")
(def urlUsername "/check-username")
(def urlPassword "/check-password")
(def url "/submit")
(def quiz-id "quiz")
(def hw-id "hw")
(def display-id "display")
(def urlQuiz "/quiz")
(def quizSub-id "quizSubmit")
(def urlSubmitQuiz "/submitQuiz")

(defn add-button []
  (d/set-style! (d/by-id "forbutton")
      "visibility" "visible"))

(defn remove-button []
  (d/set-style! (d/by-id "forbutton")
      "visibility" "hidden"))

(defn receive-result [event]
  (d/set-text! (d/by-id result-id)
               (.getResponseText (.-target event))))

(defn receive-username [event]
  (d/set-text! (d/by-id usernameResult)
               (.getResponseText (.-target event))))

(defn receive-password [event]
  (d/set-text! (d/by-id passwordResult)
               (.getResponseText (.-target event))))


(defn json-quiz-to-html [jsonQuiz]
  (let [quizNum (.-num jsonQuiz)
        quizValue (.-value jsonQuiz)
        question (.-question quizValue)
        quizId (.-id quizValue)
        quizChoices (.-choices quizValue)
        c1 (.-c1 quizChoices)
        c2 (.-c2 quizChoices)
        c3 (.-c3 quizChoices)
        c4 (.-c4 quizChoices)]
    (str "<p>" question "</p>
         <select id='" quizNum "' name='" quizId "' >
         <option value='" c1 "'>" c1 "</option>
         <option value='" c2 "'>" c2 "</option>
         <option value='" c3 "'>" c3 "</option>
         <option value='" c4 "'>" c4 "</option>
       </select>")))

(defn json-quizes-to-html [jsonArray]
  (reduce str (map json-quiz-to-html jsonArray)))

(defn get-html [jsonArray]
  (str (json-quizes-to-html jsonArray)))

(defn receive-post [event]
  (d/set-inner-html! (d/by-id display-id)
               (get-html (.getResponseJson (.-target event)))))

(defn receive-score [event]
  (d/set-inner-html! (d/by-id display-id)
      (.getResponseText (.-target event))))

(defn post-for-eval [expr-str]
  (xhr/send url receive-result "POST" expr-str))

(defn post-for-username [expr-str]
  (xhr/send urlUsername receive-username "POST" expr-str))

(defn post-for-password [expr-str]
  (xhr/send urlPassword receive-password "POST" expr-str))

(defn post-for-quiz [expr-str]
  (xhr/send urlQuiz receive-post "POST" expr-str))

(defn post-for-quiz-submit [expr-str]
  (xhr/send urlSubmitQuiz receive-score "POST" expr-str))

(defn get-answers []
  (let [q1 (.-name (d/by-id "Q1"))
        a1 (.-value (d/by-id "Q1"))
        q2 (.-name (d/by-id "Q2"))
        a2 (.-value (d/by-id "Q2"))
        q3 (.-name (d/by-id "Q3"))
        a3 (.-value (d/by-id "Q3"))]
    (str {:Q1 {:id q1 :answer a1}
         :Q2 {:id q2 :answer a2}
         :Q3 {:id q3 :answer a3}})))

(defn get-expr []
  (let [username (.-value (d/by-id username-id))
        password (.-value (d/by-id password-id))]
  (str {:username username :password password})))

(defn get-quiz []
  (let [num (.-value (d/by-id quiz-id))]
  (str {:quiz num})))

(defn ^:export main []
  (events/listen! (d/by-id button-id)
                  :click
                  (fn [event]
                    (post-for-eval (get-expr))
                    (events/stop-propagation event)
                    (events/prevent-default event)))
  (events/listen! (d/by-id quiz-id)
                  :change
                  (fn [event]
                    (post-for-quiz (get-quiz))
                    (add-button)
                    (events/stop-propagation event)
                    (events/prevent-default event)))
  (events/listen! (d/by-id quizSub-id)
                  :click
                  (fn [event]
                    (post-for-quiz-submit (get-answers))
                    (remove-button)
                    (events/stop-propagation event)
                    (events/prevent-default event)))

  (events/listen! (d/by-id username-id)
                  :keyup
                  (fn [event]
                    (post-for-username (get-expr))
                    (events/stop-propagation event)
                    (events/prevent-default event)))
  (events/listen! (d/by-id password-id)
                  :keyup
                  (fn [event]
                    (post-for-password (get-expr))
                    (events/stop-propagation event)
                    (events/prevent-default event))))
