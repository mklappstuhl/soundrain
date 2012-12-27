(ns scripper.core
  (:import [org.jaudiotagger.audio AudioFileIO]
           [org.jaudiotagger.tag FieldKey]
           [org.jaudiotagger.tag.id3 AbstractID3v2Tag]
           [org.jaudiotagger.tag.id3.framebody FrameBodyAPIC]
           [org.jaudiotagger.tag.id3 	ID3v23Frame]
           [org.jaudiotagger.tag.datatype DataTypes]
           [org.jaudiotagger.tag.reference PictureTypes]
           [org.jaudiotagger.tag TagField]
           [org.jaudiotagger.audio.mp3 MP3File]
           [org.apache.commons.lang StringEscapeUtils])
  (:require[clojure.java.io :as io]
	   [clj-http.client :as client]
           [net.cgrand.enlive-html :as html]))


(defn set-image [image]
  "returns a id3v2-frame containing image"
  (let [body (new FrameBodyAPIC)
        frame (new ID3v23Frame "APIC")]
    (.setObjectValue body DataTypes/OBJ_PICTURE_DATA image)
    (.setObjectValue body DataTypes/OBJ_PICTURE_TYPE PictureTypes/DEFAULT_ID)
    (.setObjectValue body DataTypes/OBJ_MIME_TYPE "image/jpg")
    (.setObjectValue body DataTypes/OBJ_DESCRIPTION "")
    (.setBody frame body)
    frame))

(defn create-mp3 [mp3 filename]
  "will create the new MP3 file"
  (with-open [output (new java.io.FileOutputStream filename)]
    (.write output mp3))
  (new java.io.File filename))

(defn download-binary [url]
  "downloads a file and returns it as a bytearray"
  (let [u (new java.net.URL url)
        filestream (.openStream u)]
    (org.apache.commons.io.IOUtils/toByteArray filestream)))

(defn tagmp3 [tags]
  "creates the file and tags it with the given tags"
  (let [
  filename (str "./" (:artist tags) " - " (:title tags) ".mp3")
  file (-> (:mp3 tags) download-binary  ,, (create-mp3 ,, filename) AudioFileIO/read)
  tag (.getTagOrCreateAndSetDefault file)]
    (.setField tag FieldKey/ARTIST (:artist tags))
    (.setField tag FieldKey/TITLE  (:title tags))
    (.setField tag FieldKey/YEAR   (:year tags))
    (.setField tag FieldKey/ALBUM  (:album tags))
    (.setField tag (-> (:image tags) download-binary set-image))
    (.commit file)))


(defn test-write []
  (tagmp3 {
    :mp3    "https://dl.dropbox.com/u/1994140/testmp3.mp3"
    :artist "TestArtist",
    :title  "TestTrack",
    :album  "TestAlbum",
    :year   "1000"
    :image  "https://dl.dropbox.com/u/1994140/P8270580n.jpg"}))
    
(defn get-javascripts [htmlresource]
  "returns a list of all the javascripts embedded in the body"
  (map :content (html/select htmlresource [:script])))

(defn get-artworks [htmlresource]
  "returns a list of hashs with the artwork-images"
  (let [re #"http://i1.sndcdn.com/artworks[^\"]*\.jpg"]
    (map #(hash-map :image %)	
      (filter #(not (nil? %))
	(map  #(first (re-seq re %)) 
	  (filter #(not (nil? %)) 
	    (map  #(:style (:attrs %)) 
	      (html/select htmlresource [:a] ))))))))

(defn merge-hashs [jsons artworks]
  "takes two lists of hashs and merge the corresponding hashs, returns a list of hashs"
  (map #(merge (first %) (second %)) (partition 2 (interleave artworks jsons))))

(defn extract-jsons [jscript]
  (let [re #"\{[^}]*\{[^}]*\}[^}]*\}"]
    (filter #(not (nil? %)) (flatten (map #(re-seq re %) (map str (flatten jscript)))))))

(defn filter-jsons [jsons]
  "returns only the json data that's in the given javascript"
  (let [re #"\"\w*\":\s*\"[^\"]*\""]
    (map #(re-seq re %) (flatten jsons))))

(defn json-to-hash [json]
  "takes a json in the form of \"key\":\"value\" and returns :key value, the jsons are already in a list"
  (apply hash-map (flatten 
    (map #(list (keyword (first %)) (second %)) 
      (partition 2 
	(map #(clojure.string/replace % #"\"" "") 
	  (flatten (map #(clojure.string/split % #"\":\"") json))))))))

(defn get-text-tags [htmlresource]
  "takes a html-resource and returns a list of hashs of all the text-tags"
  (filter :streamUrl (map json-to-hash (filter-jsons (extract-jsons (get-javascripts htmlresource))))))

(defn get-songs [url]
  "takes a url and returns hashs about the songs on the page"
  (let [
    htmlresource (html/html-resource (java.io.StringReader. (:body (client/get url))))
    artworks (get-artworks htmlresource )
    text-tags (get-text-tags htmlresource )]
      (merge-hashs artworks text-tags)))
  
(defn download-helper [tags]
  (let [
    artist (StringEscapeUtils/unescapeHtml (:username tags))
    title (StringEscapeUtils/unescapeHtml(clojure.string/replace (:title tags) #"[\\/.=]" "-"))
    album (StringEscapeUtils/unescapeHtml(:username tags))
    year "2012"
    mp3 (:streamUrl tags)
    image (:image tags)]
      (if (:streamUrl tags)
      (tagmp3 {
	:artist artist
	:mp3 mp3
	:title title
	:album album
	:year year
	:image (clojure.string/replace image #"badge" "t120x120")})
      (println "No streamUrl."))))
      
(defn download-mp3 [url]
  (map download-helper (get-songs url)))

(defn test-fetch []
  (download-mp3 "https://soundcloud.com/porter-robinson/porter-robinson-mat-zo-easy"))
    
(defn -main
  "I don't do a whole lot."
  [& args]
  (test-fetch))

