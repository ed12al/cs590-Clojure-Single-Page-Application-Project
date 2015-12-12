(ns gradesheet.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [ring.util.response :as ring-resp]
            [gradesheet.layout :as layout]
            [gradesheet.model.user :as user]
            [gradesheet.model.quiz :as quiz]))

(def upper (re-pattern "[A-Z]+"))
(def number (re-pattern "[0-9]+"))
(def special (re-pattern "[\"'!@#$%^&*()?]+"))

(defn strength? [password]
  (and (re-find upper password)
       (re-find number password)
       (re-find special password)))

(defn length? [password]
  (> (count password) 8))

(defn valid-password? [password]
  (and (strength? password) (length? password)))


(defn about-page
  [request]
  (ring-resp/response (format "Clojure %s - served from %s"
                              (clojure-version)
                              (route/url-for ::about-page))))

(defn quiz-page
  [request]
  (try
    (let [inputs (read-string (slurp (:body request)))
          num (Integer. (:quiz inputs))]
          (bootstrap/json-response {:num num :questions (quiz/get-quiz-questions num)}))
    (catch Throwable t
      (ring-resp/response "quiz not found"))))

(defn home-page
  [request]
  (layout/render "home.html"))

(defn register-page
  [request]
  (layout/render "register.html"))

(defn submit-quiz
  [request]
  (try
    (let [inputs (read-string (slurp (:body request)))
          quizID (Integer. (:id inputs))
          answers (:answers inputs)]
        (ring-resp/response (str (quiz/get-score quizID answers))))
    (catch Throwable t
      (ring-resp/response "Internal error"))))

(defn check-username
  [request]
  (try
    (let [inputs (read-string (slurp (:body request)))
          username (String. (:username inputs))]
        (if (user/exist-user? username)
          (ring-resp/response "username exists")
          (ring-resp/response "available")))
    (catch Throwable t
      (ring-resp/response "username cannot be empty"))))

(defn check-password
  [request]
  (try
    (let [inputs (read-string (slurp (:body request)))
          password (String. (:password inputs))]
        (if (valid-password? password)
          (ring-resp/response "strong password")
          (ring-resp/response "weak password")))
    (catch Throwable t
      (ring-resp/response "password cannot be empty"))))


(defn submit
  [request]
  (try
    (let [inputs (read-string (slurp (:body request)))
          username (String. (:username inputs))
          password (String. (:password inputs))]
          (if (and (not (user/exist-user? username)) (valid-password? password))
            (do
              (user/add-user {:username username :password password})
              (ring-resp/response "successfully registered"))
            (ring-resp/response "failed to register")))
    (catch Throwable t
      (ring-resp/response "failed to register"))))

(defn getQuizNumber
  [request]
  (bootstrap/json-response {:ids (quiz/get-quiz-ids)}))

(defroutes routes
  ;; Defines "/" and "/about" routes with their associated :get handlers.
  ;; The interceptors defined after the verb map (e.g., {:get home-page}
  ;; apply to / and its children (/about).
  [[["/" {:get home-page}
     ;;^:interceptors [(body-params/body-params) bootstrap/html-body]
     ["/about" {:get about-page}]
     ["/register" {:get register-page}]
     ["/check-username" {:post check-username}]
     ["/check-password" {:post check-password}]
     ["/register" {:post submit}]
     ["/quiz" {:post quiz-page}]
     ["/submitQuiz" {:post submit-quiz}]
     ["/getQuizCount" {:post getQuizNumber}]]]])

;; Consumed by gradesheet.server/create-server
;; See bootstrap/default-interceptors for additional options you can configure
(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::bootstrap/interceptors []
              ::bootstrap/routes routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::bootstrap/allowed-origins ["scheme://host:port"]

              ;; Root for resource interceptor that is available by default.
              ::bootstrap/resource-path "/public"

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ::bootstrap/type :jetty
              ;;::bootstrap/host "localhost"
              ::bootstrap/port 8080})

