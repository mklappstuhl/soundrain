(ns scripper.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]))
(load "core")

(defroutes app-routes
  (GET "/" [] "Hello World")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))