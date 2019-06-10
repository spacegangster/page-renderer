(ns page-renderer.core
  (:require [garden.core :refer [css]]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [hiccup.page :refer [html5]]
            [page-renderer.util :as u]))


(defn or-text [& texts]
  (first (filter seq texts)))

(defn update-if-present [m k f & args]
  (if (contains? m k)
    (assoc m k (apply f (cons (get m k) args)))
    m))

(defn m [meta-name meta-value]
  (if meta-value
    (str "<meta name=\"" (name meta-name) "\" content=\"" meta-value"\">")))

(defn mp [meta-name meta-value]
  (if meta-value
    (str "<meta property=\"" (name meta-name) "\" content=\"" meta-value"\">")))

(defn- -get-filepath [asset-path]
  (str "resources/public" asset-path))

(def ^:private default-mtime (System/currentTimeMillis))

(defn try-get-mtime-for-a-file [web-asset-path]
  (let [file (io/file (str "resources/public" web-asset-path))]
    (if (.exists file)
      (.lastModified file)
      default-mtime)))

(defn- mtime-or-default [web-asset-path]
  (if-let [resource (io/resource (str "public" web-asset-path))]
    (try
      (.. resource openConnection getLastModified)
      (catch Exception e
        default-mtime))
    (try-get-mtime-for-a-file web-asset-path)))

(defn cache-bust [web-asset-path]
  (let [is-relative-path? (not (re-find #"^\/" web-asset-path))]
    (str web-asset-path
         "?mtime="
         (if is-relative-path?
           default-mtime
           (mtime-or-default web-asset-path)))))


(defn twitter-meta [{:keys [twitter-site twitter-card-type twitter-title twitter-creator
                            twitter-description twitter-image twitter-image-alt] :as renderable}]
  (when twitter-site
  (list
    (m "twitter:card"        twitter-card-type)
    (m "twitter:creator"     twitter-creator)
    (m "twitter:site"        twitter-site)
    (m "twitter:title"       twitter-title)
    (m "twitter:description" twitter-description)
    (m "twitter:image"       twitter-image)
    (m "twitter:image:alt"   twitter-image-alt))))


(defn og-meta [{:keys [og-image og-title og-description og-url og-type] :as renderable}]
  (list
    (mp "og:title"       og-title)
    (mp "og:url"         og-url)
    (mp "og:type"        og-type)
    (mp "og:description" og-description)
    (mp "og:image"       og-image)))


(defn- provide-default-props [{:keys [twitter-description twitter-title twitter-image twitter-card-type
                                      og-description meta-description meta-social-description description
                                      title og-title meta-title meta-social-title
                                      og-image meta-og-image] :as renderable}]
  (assoc renderable
         :favicon             (or-text (:favicon renderable) "/favicon.png")
         :meta-description    (or-text meta-description meta-social-description og-description description)
         ;
         :twitter-title       (or-text twitter-title meta-social-title og-title)
         :twitter-image       (or-text twitter-image og-image meta-og-image)
         :twitter-description (or-text twitter-description meta-social-description meta-description description)
         :twitter-card-type   (or-text twitter-card-type "summary")
         ;
         :og-title            (or-text og-title meta-social-title meta-title title)
         :og-image            (or-text og-image meta-og-image)
         :og-description      (or-text og-description meta-social-description meta-description description
         :meta-description    (or-text meta-description meta-social-description og-description))))

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


(defn- -render-mp-entry [[mp-key mp-value]]
  (mp mp-key mp-value))

(defn- render-meta-props [meta-props]
  (map -render-mp-entry meta-props))


(defn attr-append [s [k v]] (str s " " (name k) "=" (name v)))

(defn render-attrs [attrs]
  (reduce attr-append "" attrs))

(defn cache-bust-assets [page-data]
  (-> page-data
      (update-if-present :twitter-image cache-bust)
      (update-if-present :favicon cache-bust)
      (update-if-present :og-image cache-bust)))


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
   @param {string} renderable.og-type
   @param {string} renderable.og-url - OpenGraph page permalink

   @param {map} meta-props – meta which must be rendered as props
    {'fb:app_id' 123}

   @param {string}  renderable.twitter-site - twitter @username
   @param {keyword} renderable.twitter-card-type - twitter card type
    [:summary (default), :summary_large_image, :app, :player]
   @param {string}  renderable.twitter-description - twitter card description
   @param {string}  renderable.twitter-image - twitter image
   @param {string}  renderable.twitter-image-alt - twitter image alt

   @param {string} renderable.garden-css - data structure for Garden CSS
   @param {string} renderable.stylesheet - stylesheet filename, will be plugged into the head, will cause
    browser waiting for download.
   @param {string/collection<string>} renderable.stylesheet-inline - stylesheet filename, will be inlined into the head.
   @param {string} renderable.script - script name
   @param {string} renderable.stylesheet-async - stylesheet filename, will be loaded asynchronously by script.
   @param {string} renderable.script - script name, will be loaded asynchronously
   @param {string} renderable.script-sync - script name, will be loaded synchronously
   @param {string} renderable.head-tags - data structure to render into HTML of the document's head"
  [renderable]
  (let [renderable (-> renderable
                       provide-default-props
                       cache-bust-assets)
        {:keys [body title head-tags
                garden-css
                stylesheet stylesheet-inline stylesheet-async
                script script-sync
                doc-attrs js-module favicon
                meta-title meta-description meta-keywords
                livereload-script?
                ]} renderable
        title      (or meta-title title)
        analytics? (get renderable :analytics? true)
        inline-css (if garden-css (css garden-css))]
    (str
      "<!DOCTYPE html>"
      "<html " (render-attrs doc-attrs) ">"
      (hiccup/html
        [:head

         [:meta {:charset "utf-8"}]
         [:link {:rel "icon", :type "image/png", :href favicon}]
         (m "viewport", "width=device-width, initial-scale=1, maximum-scale=1")
         (if livereload-script?
           [:script {:src "//localhost:35729/livereload.js?snipver=2" :async true}])
         (seq head-tags)
         ;
         (render-scripts--async script)
         (render-scripts--sync script-sync)
         (if js-module [:script {:type "module" :src (cache-bust js-module), :async true}])
         ;
         [:title title]
         (m :description meta-description)
         (m :keywords meta-keywords)
         (render-meta-props (:meta-props renderable))
         (:meta-tags renderable)
         (twitter-meta renderable)
         (og-meta renderable)

         ; Inline CSS and stylesheets
         (render-stylesheets stylesheet)
         (if inline-css [:style#inline-css--garden inline-css])
         (render-inline-sheets stylesheet-inline)])
      (hiccup/html
        [:body body
         (if stylesheet-async
           (u/make-stylesheet-appender stylesheet-async))])
      "</html")))

(defn respond-page
  "Renders a page and returns basic Ring response map"
  [renderable]
  {:status  200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (render-page renderable)})
