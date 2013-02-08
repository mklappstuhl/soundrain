(ns scripper.util
  (:import [org.apache.commons.io IOUtils])
  (:require [compojure.handler :as handler]))
  
  (defn download-binary [url]
    "downloads a file and returns it as a bytearray"
    (let [stream     (new java.net.URL url)
          filestream (.openStream stream)]
      (IOUtils/toByteArray filestream)))

  (defn with-uri-rewrite
  "Rewrites a request uri with the result of calling f with the
   request's original uri.  If f returns nil the handler is not called."
  [handler f]
  (fn [request]
    (let [uri (:uri request)
          rewrite (f uri)]
      (if rewrite
        (handler (assoc request :uri rewrite))
        nil))))
  
  (defn- uri-snip-slash
    "Removes a trailing slash from all uris except \"/\"."
    [uri]
    (if (and (not (= "/" uri))
             (.endsWith uri "/"))
      (subs uri 0 (dec (count uri)))
      uri))
  
  (defn ignore-trailing-slash
    "Makes routes match regardless of whether or not a uri ends in a slash."
    [handler]
    (with-uri-rewrite handler uri-snip-slash))