(ns scripper.view
  (:use compojure.core scripper.core [hiccup core page form util] ))
  (use 'hiccup.bootstrap.page)
  
(defn html-doc [title & body] 
  (html 
    (doctype :html4) 
    [:html 
      [:head 
        [:title title]
        (include-css "/css/style.css")
        (include-bootstrap)] 
      [:body 
       [:div {:class "body-content"}
        [:h2 {:text-align "center"} title]]
        body]])) 
        
(defn url-form []
  (html-doc "scripper" 
    [:form {:method "POST", :action (to-uri "/"), :class "form-search"}
      [:input {:type "text" :name "url" :class "input-xxlarge search-query"}]
      [:button {:type "submit" :class "btn" } "Search"]
    ]))
      
(defn song-form [tags]
  [:div.mp3field 
    [:h3 (:username tags)  (:title tags)]
    [:img {:src (to-uri (:image tags))}]
    [:img {:src (to-uri (:waveformUrl tags))}]
    [:h4 "download"]
    ])
  
(defn results [url]
  (let [songs (get-songs url)]
    (html-doc "scripper"
      (map song-form songs))))
  
  
 