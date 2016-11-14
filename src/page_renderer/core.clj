(ns page-renderer.core
  (:require [garden.core :refer [css]]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [hiccup.page :refer [html5]]))


(defn m [meta-name meta-value]
  (if meta-value
    [:meta {:name (name meta-name) :content meta-value}]))

(defn cache-bust
  "Try to cachebust the asset by supplying mtime parameter.
   Only works with absolute paths."
  [asset-path]
  (let [is-abs-path? (re-find #"^\/" asset-path)]
  (if-not is-abs-path?
    asset-path
    (let [path-in-resources (str "resources/public" asset-path)
          file (io/file path-in-resources)
          exists? (.exists file)
          mtime (if exists? (.lastModified file))
          busted-path (str asset-path "?mtime=" mtime)]
      (if exists?
        busted-path
        asset-path)))))


(defn twitter-meta [{:keys [twitter-site twitter-card-type twitter-title
                            twitter-description twitter-image twitter-image-alt
                            title og-image og-description] :as renderable}]
  (when twitter-site
  (let [twitter-card-type    (name (or twitter-card-type :summary))
        twitter-title        (or twitter-title title)
        twitter-description  (or twitter-description og-description)
        twitter-image        (or twitter-image og-image)]
  (list
    (m "twitter:card" twitter-card-type)
    (m "twitter:site" twitter-site)
    (m "twitter:description" twitter-description)
    (m "twitter:image" twitter-image)
    (m "twitter:image:alt" twitter-image-alt)
    ))))


(defn og-meta [{:keys [og-image og-title og-description og-url
                       title] :as renderable}]
  (let [og-title (or og-title title)]
  (list
    (m "og:title" og-title)
    (m "og:url" og-url)
    (m "og:description" og-description)
    (m "og:image" og-image))))

(defn- provide-default-props [{:keys [twitter-description og-description meta-description] :as renderable}]
  (assoc renderable
         :favicon (or (:favicon renderable) "/favicon.png")
         :twitter-description (or twitter-description meta-description og-description)
         :og-description (or og-description meta-description)
         :meta-description (or meta-description og-description)))


(defn render-page
  "Render a page
   @param {hash-map} renderable
   @param {vector} renderable.body - data structure for Hiccup to render into HTML of the document's body
   @param {string} renderable.meta-title - content for title tag (preferred)
   @param {string} renderable.title - content for title tag
   @param {string} renderable.meta-keywords - content for title tag
   @param {string} renderable.meta-description - meta description

   @param {string} renderable.og-title - OpenGraph title
   @param {string} renderable.og-description - OpenGraph description
   @param {string} renderable.og-image - absolute url to image for OpenGraph
   @param {string} renderable.og-url - OpenGraph page permalink

   @param {string} renderable.twitter-site - twitter @username
   @param {keyword} renderable.twitter-card-type - twitter card type
    [:summary (default), :summary_large_image, :app, :player]
   @param {string} renderable.twitter-description - twitter card description
   @param {string} renderable.twitter-image - twitter image
   @param {string} renderable.twitter-image-alt - twitter image alt

   @param {string} renderable.garden-css - data structure for Garden CSS
   @param {string} renderable.stylesheet - stylesheet filename
   @param {string} renderable.script - script name
   @param {string} renderable.head-tags - data structure to render into HTML of the document's head"
  [renderable]
  (let [renderable (provide-default-props renderable)
        {:keys [body title head-tags stylesheet script og-image garden-css
                meta-title meta-description meta-keywords favicon]} renderable
        title      (or meta-title title)
        analytics  (get renderable :analytics true)
        inline-css (if garden-css (css garden-css))]
  (html5
    [:head
      [:meta {:charset "utf-8"}]
      [:link {:rel "icon", :type "image/png", :href favicon}]
      (m "viewport", "width=device-width, initial-scale=1, maximum-scale=1")
      head-tags
      (if inline-css [:style inline-css])
      (if script [:script {:src (cache-bust script), :async true}])
      [:title title]
      (m :description meta-description)
      (m :keywords meta-keywords)
      (twitter-meta renderable)
      (og-meta renderable)
      (if stylesheet
         [:link {:rel "stylesheet" :type "text/css" :href (cache-bust stylesheet)}])]
    body)))

(defn respond-page
  "Renders a page and returns basic Ring response map"
  [renderable]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (render-page renderable)})
