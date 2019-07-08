(ns page-renderer.core
  (:require [clojure.java.io :as io]
            [garden.core :as garden]
            [hiccup.core :as hiccup]
            [page-renderer.util :as u]))


(defn- or-text [& texts]
  (first (filter seq texts)))

(defn- update-if-present [m k f]
  (if (contains? m k)
    (update m k f)
    m))

(defn assoc-some
  ; taken from weavejester/medley
  "Associates a key with a value in a map, if and only if the value is not nil."
  ([m k v]
   (if (nil? v) m (assoc m k v)))
  ([m k v & kvs]
   (reduce (fn [m [k v]] (assoc-some m k v))
           (assoc-some m k v)
           (partition 2 kvs))))

(defn- m [meta-name meta-value]
  (if meta-value
    (str "<meta name=\"" (name meta-name) "\" content=\"" meta-value"\">")))

(defn- mp [meta-name meta-value]
  (if meta-value
    (str "<meta property=\"" (name meta-name) "\" content=\"" meta-value"\">")))

(defn- -get-filepath [asset-path]
  (str "resources/public" asset-path))



(def ^:private default-mtime (System/currentTimeMillis))

(defn- try-get-mtime-for-a-file [web-asset-path]
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

(defn cache-bust-one [web-asset-path]
  (let [is-relative-path? (not (re-find #"^\/" web-asset-path))]
    (str web-asset-path
         "?mtime="
         (if is-relative-path?
           default-mtime
           (mtime-or-default web-asset-path)))))

(defn cache-bust [web-asset-path-or-coll]
  (if (coll? web-asset-path-or-coll)
    (map cache-bust-one web-asset-path-or-coll)
    (cache-bust-one web-asset-path-or-coll)))


(defn twitter-meta [{:keys [twitter-site twitter-card-type twitter-title twitter-creator
                            twitter-description twitter-image twitter-image-alt] :as renderable}]
  (when (or twitter-site twitter-creator)
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
  (assoc-some
    renderable
    :title               (or-text title meta-title)
    :meta-description    (or-text meta-description meta-social-description og-description description)
    :meta-title          (or-text meta-title title)
    ;
    :twitter-title       (or-text twitter-title meta-social-title og-title)
    :twitter-image       (or-text twitter-image og-image meta-og-image)
    :twitter-description (or-text twitter-description meta-social-description meta-description description)
    :twitter-card-type   (or-text twitter-card-type "summary")
    ;
    :og-title            (or-text og-title meta-social-title meta-title title)
    :og-image            (or-text og-image meta-og-image)
    :og-description      (or-text og-description meta-social-description meta-description description)))

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
        [:script {:src script :async async?}]))))

(defn render-scripts--sync [script-name]
  (render-scripts script-name false))

(defn render-scripts--async [script-name]
  (render-scripts script-name true))

(defn render-js-modules [src]
  (if src
    (if (coll? src)
      (for [s src]
        [:script {:type "module" :src s, :async true}])
      src)))

(defn render-stylesheets [stylesheets]
  (if stylesheets
    (if (string? stylesheets)
      (recur [stylesheets])
      (for [stylesheet stylesheets]
        [:link {:rel "stylesheet" :type "text/css" :href stylesheet}]))))


(defn- -render-mp-entry [[mp-key mp-value]]
  (mp mp-key mp-value))

(defn- render-meta-props [meta-props]
  (map -render-mp-entry meta-props))


(defn attr-append [s [k v]]
  (str s " " (name k) "=" (pr-str (name v))))

(defn render-attrs [attrs]
  (reduce attr-append "" attrs))

(defn on-dom-interactive-fragment [js-snippet]
"const __pageRendererInitId = setInterval(function(){
    if (('complete' === document.readyState) || ('interactive' === document.readyState)) {
        clearInterval(__pageRendererInitId)
				"js-snippet"
    }
}, 5)")


(defn cache-bust-assets [page-data]
  (-> page-data
      (update-if-present :twitter-image cache-bust-one)
      (update-if-present :og-image cache-bust-one)
      (update-if-present :favicon cache-bust-one)
      ;
      (update-if-present :script cache-bust)
      (update-if-present :script-sync cache-bust)
      (update-if-present :js-module cache-bust)
      ;
      (update-if-present :stylesheet cache-bust)
      (update-if-present :stylesheet-async cache-bust)
      ;
      (update-if-present :manifest cache-bust-one)))

(defn- auto-body [body opt-injection]
  (if (keyword? (first body))
    (if (.startsWith (name (first body)) "body")
      (conj body opt-injection)
      [:body body opt-injection])
    body))

(defn- async-sheets [stylesheet-async]
  (cond
    (nil? stylesheet-async)     nil
    (string? stylesheet-async) (u/make-stylesheet-appender stylesheet-async)
    (coll? stylesheet-async)   (map u/make-stylesheet-appender stylesheet-async)
    :else (throw (Exception. "not a collection or a string"))))


(defn render-page
  "Render a page
   @param {hash-map} renderable
   @param {vector} renderable.body - data structure for Hiccup to render into HTML of the document's body
   @param {string} renderable.title - content for title tag
   @param {string} renderable.lang - when provided will render a meta tag and a document attribute
    for page language.
   @param {string} renderable.meta-title - content for title tag (preferred)
   @param {string} renderable.meta-keywords - content for title tag
   @param {string} renderable.meta-description - meta description
   @param {map}    renderable.meta-props – meta which must be rendered as props
    {'fb:app_id' 123}

   @param {string} renderable.og-title - OpenGraph title
   @param {string} renderable.og-description - OpenGraph description
   @param {string} renderable.og-image - absolute url to image for OpenGraph
   @param {string} renderable.og-type
   @param {string} renderable.og-url - OpenGraph page permalink

   @param {string}  renderable.twitter-site - twitter @username. Required for all Twitter meta to render
   @param {string}  renderable.twitter-creator - twitter @username.
   @param {keyword} renderable.twitter-card-type - twitter card type
    one of #{:summary  :summary_large_image :app :player}
   @param {string}  renderable.twitter-description - twitter card description
   @param {string}  renderable.twitter-image - twitter image link. Twitter images are useu
   @param {string}  renderable.twitter-image-alt - twitter image alt

   @param {string/boolean} renderable.manifest - truthy value will add a manifest link.
    If a string is passed – it'll be treated as a manifest url. Otherwise '/manifest.json'
    will be specified.
   @param {string} renderable.garden-css - data structure for Garden CSS
   @param {string} renderable.stylesheet - stylesheet filename, will be plugged into the head, will cause
    browser waiting for download.
   @param {string/collection<string>} renderable.stylesheet-inline - stylesheet filename, will be inlined into the head.
   @param {string} renderable.stylesheet-async - stylesheet filename, will be loaded asynchronously by script.
   @param {string} renderable.script - script name, will be loaded asynchronously
   @param {string} renderable.script-sync - script name, will be loaded synchronously
   @param {string} renderable.js-module - entry point for JS modules. If you prefer your scripts to be served as modules
   @param {string} renderable.on-dom-interactive-js - a js snippet to run once DOM is interactive or ready.
   @param {string} renderable.head-tags - data structure to render into HTML of the document's head"
  [renderable]
  (let [renderable (-> renderable
                       (update-if-present :manifest #(if (string? %) % "/manifest.json"))
                       (assoc :favicon (or-text (:favicon renderable) "/favicon.png"))
                       cache-bust-assets
                       provide-default-props)
        {:keys [body title head-tags
                garden-css
                stylesheet stylesheet-inline stylesheet-async
                script script-sync on-dom-interactive-js
                doc-attrs js-module favicon
                lang
                manifest
                meta-title meta-description meta-keywords
                livereload-script?]} renderable
        doc-attrs  (assoc-some doc-attrs :lang lang)
        title      (or meta-title title)
        inline-css (if garden-css (garden/css garden-css))]
    (str
      "<!DOCTYPE html>"
      "<html " (render-attrs doc-attrs) ">"
      (hiccup/html
        [:head

         [:meta {:charset "utf-8"}]
         (m "viewport", "width=device-width, initial-scale=1, maximum-scale=1")
         [:link {:rel "icon", :type "image/png", :href favicon}]

         (if lang
           [:meta {:http-equiv "Content-Language" :content lang}])

         (if manifest
           [:link {:rel "manifest" :href (if (string? manifest) manifest "/manifest.json")}])

         (if livereload-script?
           [:script {:src "//localhost:35729/livereload.js?snipver=2" :async true}])

         (if (seq on-dom-interactive-js)
           (on-dom-interactive-fragment on-dom-interactive-js))

         (seq head-tags)
         ;
         (render-scripts--async script)
         (render-scripts--sync script-sync)
         (render-js-modules js-module)
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
      (hiccup/html (auto-body body (async-sheets stylesheet-async)))
      "</html>")))

(defn respond-page
  "Renders a page and returns basic Ring response map"
  [renderable]
  {:status  200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (render-page renderable)})
