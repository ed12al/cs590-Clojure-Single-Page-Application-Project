(ns gradesheet.model.user
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [monger.result :refer [ok? has-error?]]))

(def conn (mg/connect))
(def db (mg/get-db conn "project")) ;; database name
(def document "user") ;; document

(defn add-user
  [quiz-result]
  (mc/insert-and-return db document quiz-result))

(defn get-user
  [search-criteria]
  (mc/find-maps db document search-criteria))

(defn get-user-token
  [username]
  (let [tmap (mc/find-one-as-map db document {:username username} ["token"])]
    (:token tmap)))

(defn update-user
  [username,token]
  (mc/update db document {:username (username :username) } {$set {:token (token :token) }}))

(defn exist-user?
  [username]
  (if (empty? (get-user {:username username}))
    false
    true))

(defn auth-user?
  [username password]
  (if (empty? (get-user {:username username :password password}))
    false
    true))
