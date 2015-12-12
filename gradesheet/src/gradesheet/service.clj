(ns gradesheet.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.interceptor.helpers :refer [definterceptor]]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [ring.middleware.session.cookie :as cookie]
            [ring.util.response :as ring-resp]
            [gradesheet.layout :as layout]
            [gradesheet.model.user :as user]
            [gradesheet.model.quiz :as quiz]
            [clj-oauth2.client :as oauth2]
           [noir.response :as resp]
           [clj-http.client :as client]
           [cheshire.core :as parse]
            ))

(def upper (re-pattern "[A-Z]+"))
(def number (re-pattern "[0-9]+"))
(def special (re-pattern "[\"'!@#$%^&*()?]+"))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn strength? [password]
  (and (re-find upper password)
       (re-find number password)
       (re-find special password)))

(defn length? [password]
  (> (count password) 8))

(defn valid-password? [password]
  (and (strength? password) (length? password)))


(defn login-page
  [request]
  (layout/render "register.html"))

(defn quiz-page
  [request]
  (try
    (let [inputs (read-string (slurp (:body request)))
          num (Integer. (:quiz inputs))]
          (bootstrap/json-response {:num num :questions (quiz/get-quiz-questions num)}))
    (catch Throwable t
      (ring-resp/response "quiz not found"))))

(defn auth?
  [request]
  (let [token (get-in request [:session :token])
        username (get-in request [:session :username])]
    (user/valid-token? username token)))

(defn home-page
  [request]
  (if (auth? request)
    (layout/render "home.html")
    (ring-resp/redirect "/login")))

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
      (clojure.pprint/pprint password)
        (if (not (valid-password? password))
          (ring-resp/response "weak password")
          (ring-resp/response "strong password")))
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

(defn validate-user
  [request]

  (try
    (let [form (:form-params (body-params/form-parser request))
          username (String. (get form "username"))
          password (String. (get form "password"))]

          (if (user/auth-user? username password)
            (do
              (user/update-user username (uuid))
              (-> (ring-resp/redirect "/")
                  (assoc :session {:username username :token (user/get-user-token username)})))

              (ring-resp/redirect "/login")))

    (catch Throwable t
      (ring-resp/response "failed to register"))))


(defn register-user
  [request]

  (try
    (let [form (:form-params (body-params/form-parser request))
          username (String. (get form "username-register"))
          email (String. (get form "email-register"))
          password (String. (get form "password-register"))]

      (user/add-user {:username username :password password :email email})
         ;; (if (user/auth-user? username password)
            ;;(ring-resp/response "successfully login")
            (ring-resp/redirect "/login")
            )

    (catch Throwable t
      (ring-resp/response "failed to register"))))

(defn logout
  [request]
  (-> (ring-resp/redirect "/login")
      (assoc :session {:username nil :token nil})))

(def facebook-user
  (atom {:facebook-id "" :facebook-name "" :facebook-email ""}))

(def APP_ID "942931509114622")
(def APP_SECRET "74293a60159d5d4b13a6cf0074101118")
(def REDIRECT_URI "http://localhost:3030/auth_facebook")

(def facebook-oauth2
 {:authorization-uri "https://graph.facebook.com/oauth/authorize"
  :access-token-uri "https://graph.facebook.com/oauth/access_token"
  :redirect-uri REDIRECT_URI
  :client-id APP_ID
  :client-secret APP_SECRET
  :access-query-param :access_token
  :scope ["email"]
  :grant-type "authorization_code"})

(defn call_fb[request]
(resp/redirect
  (:uri (oauth2/make-auth-request facebook-oauth2)))
)


(defn facebook [request]
  (if (= "access_denied" (get-in request [:query-params :error]))
    (ring-resp/redirect "/login")
 (let [access-token-response  (:body (client/get (str "https://graph.facebook.com/oauth/access_token?"
                                                    "client_id=" APP_ID
                                                  "&redirect_uri=" REDIRECT_URI
                                                    "&client_secret=" APP_SECRET
                                                "&code=" (get-in request[:params :code] ))))

       access-token (get (re-find #"access_token=(.*?)&expires=" access-token-response) 1)
       user-details (-> (client/get (str "https://graph.facebook.com/me?access_token=" access-token))
                        :body
                       (parse/parse-string))

       token (uuid)
       username (str (get user-details "id"))]
   (user/update-token-others username token)
    (-> (ring-resp/redirect "/")
      (assoc :session {:username username :token token})
;;(swap! facebook-user
  ;; #(assoc % :facebook-id %2 :facebook-name %3 :facebook-email %4)
    ;; (get user-details "id")
     ;;(get user-details "first_name")
     ;;(get user-details "email"))
   ))))




(def CLIENT_ID_GOOGLE "569865038784-d3oftdq3heetu1k9rvs27nho1lnj9st0.apps.googleusercontent.com")
(def REDIRECT_URI_GOOGLE "http://localhost:3030/auth_google")
(def login-uri_GOOGLE "https://accounts.google.com")
(def CLIENT_SECRET_GOOGLE "v0nwnTlxkRrAQCU10aNkZg7a")
(def google-user (atom {:google-id "" :google-name "" :google-email ""}))

(def red
  (str "https://accounts.google.com/o/oauth2/auth?"
              "scope=email%20profile&"
              "redirect_uri=" (ring.util.codec/url-encode REDIRECT_URI_GOOGLE) "&"
              "response_type=code&"
              "client_id=" (ring.util.codec/url-encode CLIENT_ID_GOOGLE) "&"
              "approval_prompt=force"))

(defn google [params]
  (if (= "access_denied" (get-in params [:params :error]))
    (ring-resp/redirect "/login")
  (let [access-token-response (client/post "https://accounts.google.com/o/oauth2/token"
                                          {:form-params {:code (get-in params[:params :code] )
                                           :client_id CLIENT_ID_GOOGLE
                                           :client_secret CLIENT_SECRET_GOOGLE
                                           :redirect_uri REDIRECT_URI_GOOGLE
                                           :grant_type "authorization_code"}})
       user-details (parse/parse-string (:body (client/get (str "https://www.googleapis.com/oauth2/v1/userinfo?access_token="
 (get (parse/parse-string (:body access-token-response)) "access_token")))))
        username (get user-details "id")
        token (uuid)]
 ;;(swap! google-user #(assoc % :google-id %2 :google-name %3 :google-email %4)
        ;;(get user-details "id") (get user-details "name") (get user-details "email"))
    (user/update-token-others username token)
  (-> (ring-resp/redirect "/")
      (assoc :session {:username username :token token})
      ))))


(defn call_google [request]
  (resp/redirect red)
  )


(definterceptor session-interceptor
  (middlewares/session {:store (cookie/cookie-store)}))

(defroutes routes
  ;; Defines "/" and "/about" routes with their associated :get handlers.
  ;; The interceptors defined after the verb map (e.g., {:get home-page}
  ;; apply to / and its children (/about).
  [[[ "/"
       ^:interceptors [session-interceptor]
      {:get home-page}]

     ["/login" {:get login-page}]
     ["/register" {:get register-page}]
     ["/check-username" {:post check-username}]
     ["/check-password" {:post check-password}]
     ["/validate" ^:interceptors [middlewares/params middlewares/keyword-params session-interceptor] {:post validate-user}]
     ["/register" {:post register-user}]
     ["/getfb" {:get call_fb}]
     ["/auth_facebook" ^:interceptors [session-interceptor] {:get facebook}]
     ["/google" {:get call_google}]
     ["/auth_google" ^:interceptors [session-interceptor] {:get google}]
     ["/logout" ^:interceptors [middlewares/params middlewares/keyword-params session-interceptor] {:post logout}]
     ["/quiz" ^:interceptors [session-interceptor] {:post quiz-page}]
     ["/submitQuiz" ^:interceptors [session-interceptor] {:post submit-quiz}]
     ["/getQuizCount" ^:interceptors [session-interceptor] {:post getQuizNumber}]
     ]])

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
              ::bootstrap/port 3030})

