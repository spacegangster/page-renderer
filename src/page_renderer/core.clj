(ns page-renderer.core
  (:require [garden.core :refer [css]]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [hiccup.page :refer [html5]]
            [page-renderer.util :as u]))


(defn or-text [& texts]
  (first (filter seq texts)))

(defn m [meta-name meta-value]
  (if meta-value
    [:meta {:name (name meta-name) :content meta-value}]))

(defn- -get-filepath [asset-path]
  (str "resources/public" asset-path))

(def launch-time (System/currentTimeMillis))

(defn cache-bust [asset-path]
  (let [is-abs-path? (re-find #"^\/" asset-path)]
  (if-not is-abs-path?
    (str asset-path "?mtime=" launch-time)
    (let [path-in-resources (-get-filepath asset-path)
          file              (io/file path-in-resources)
          exists?           (.exists file)
          mtime             (if exists? (.lastModified file))
          busted-path       (str asset-path "?mtime=" mtime)]
      (if exists?
        busted-path
        asset-path)))))


(defn twitter-meta [{:keys [twitter-site twitter-card-type twitter-title
                            twitter-description twitter-image twitter-image-alt] :as renderable}]
  (when twitter-site
  (list
    (m "twitter:card"        twitter-card-type)
    (m "twitter:site"        twitter-site)
    (m "twitter:title"       twitter-title)
    (m "twitter:description" twitter-description)
    (m "twitter:image"       twitter-image)
    (m "twitter:image:alt"   twitter-image-alt)
    )))


(defn og-meta [{:keys [og-image og-title og-description og-url
                       title] :as renderable}]
  (let [og-title (or og-title title)]
  (list
    (m "og:title" og-title)
    (m "og:url" og-url)
    (m "og:description" og-description)
    (m "og:image" og-image))))

(defn- fix-underscore-keys [d]
  (let [uscore-keys [:meta_title :meta_description :meta_og_image :meta_keywords
                     :meta_tags :meta_social_description :meta_social_title]
        fix-key (fn [k] (keyword (s/replace (name k) "_" "-")))
        assoc-key
          (fn [obj k]
            (if (and (contains? d k) (not (contains? obj (fix-key k))))
              (dissoc (assoc obj (fix-key k) (d k)) k)
              obj))]
  (reduce assoc-key d uscore-keys)))

(defn- provide-default-props [{:keys [twitter-description twitter-title twitter-image twitter-card-type
                                      og-description meta-description meta-social-description
                                      title og-title meta-title meta-social-title
                                      og-image meta-og-image] :as renderable}]
  (assoc renderable
         :favicon             (or-text (:favicon renderable) "/favicon.png")
         :twitter-title       (or-text twitter-title meta-social-title og-title)
         :twitter-image       (or-text twitter-image og-image meta-og-image)
         :twitter-description (or-text twitter-description meta-social-description meta-description)
         :twitter-card-type   (or-text twitter-card-type "summary")
         :og-title            (or-text og-title meta-social-title meta-title title)
         :og-image            (or-text og-image meta-og-image)
         :og-description      (or-text og-description meta-social-description meta-description)
         :meta-description    (or-text meta-description meta-social-description og-description)))

(defn render-inline-sheets [filepath-or-vec]
  (if (string? filepath-or-vec)
    (render-inline-sheets [filepath-or-vec])
    (for [filepath filepath-or-vec]
      [:style (slurp (-get-filepath filepath))])))

(defn render-scripts [script-sync async?]
  (if script-sync
    (if (string? script-sync)
      (recur [script-sync] async?)
      (for [script script-sync]
        [:script {:src (cache-bust script) :async async?}]))))

(defn render-scripts--sync [script-name]
  (render-scripts script-name false))

(defn render-scripts--async [script-name]
  (render-scripts script-name true))

(defn render-stylesheets [stylesheets]
  (if stylesheets
    (if (string? stylesheets)
      (recur [stylesheets])
      (for [stylesheet stylesheets]
        [:link {:rel "stylesheet" :type "text/css" :href (cache-bust stylesheet)}]))))


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
   @param {string} renderable.stylesheet - stylesheet filename, will be plugged into the head, will cause
    browser waiting for download.
   @param {string/collection<string>} renderable.stylesheet-inline - stylesheet filename, will be inlined into the head.
   @param {string} renderable.stylesheet-async - stylesheet filename, will be loaded asynchronously by script.
   @param {string} renderable.script - script name, will be loaded asynchronously
   @param {string} renderable.script-sync - script name, will be loaded synchronously
   @param {string} renderable.head-tags - data structure to render into HTML of the document's head"
  [renderable]
  (let [renderable (-> renderable fix-underscore-keys provide-default-props)
        {:keys [body title head-tags
                garden-css
                stylesheet stylesheet-inline stylesheet-async
                script script-sync
                meta-title meta-description meta-keywords favicon]} renderable
        title      (or meta-title title)
        analytics?  (get renderable :analytics? true)
        inline-css (if garden-css (css garden-css))]
  (html5
    [:head
      [:meta {:charset "utf-8"}]
      [:link {:rel "icon", :type "image/png", :href (cache-bust favicon)}]
      (m "viewport", "width=device-width, initial-scale=1, maximum-scale=1")
      (seq head-tags)
      ;
      (render-scripts--async script)
      (render-scripts--sync script-sync)
      ;
      [:title title]
      (m :description meta-description)
      (m :keywords meta-keywords)
      (:meta-tags renderable)
      (twitter-meta renderable)
      (og-meta renderable)

      ; Inline CSS and stylesheets
      (render-stylesheets stylesheet)
      ]
      (if inline-css [:style#inline-css--garden inline-css])
      (render-inline-sheets stylesheet-inline)
    (conj body
          (if stylesheet-async
            (u/make-stylesheet-appender stylesheet-async))))))

(defn respond-page
  "Renders a page and returns basic Ring response map"
  [renderable]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (render-page renderable)})
