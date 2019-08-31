(ns page-renderer.core
  (:require [garden.core :as garden]
            [hiccup.core :as hiccup]
            [page-renderer.service-worker-lifecycle :as swl]
            [page-renderer.cachebusting :as cb]
            [page-renderer.util :as u])
  (:import (java.util Map)))


(defn- m [meta-name meta-value]
  (if meta-value
    (str "<meta name=\"" (name meta-name) "\" content=\"" meta-value"\">")))

(defn- mp [meta-name meta-value]
  (if meta-value
    (str "<meta property=\"" (name meta-name) "\" content=\"" meta-value"\">")))

(defn- -get-filepath [asset-path]
  (str "resources/public" asset-path))


(defn- twitter-meta [{:keys [twitter-site twitter-card-type twitter-title twitter-creator
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

(defn- og-meta [{:keys [og-image og-title og-description og-url og-type] :as renderable}]
  (list
    (mp "og:title"       og-title)
    (mp "og:url"         og-url)
    (mp "og:type"        og-type)
    (mp "og:description" og-description)
    (mp "og:image"       og-image)))



(defn- provide-default-props [{:keys [twitter-description twitter-title twitter-image twitter-card-type
                                      og-description meta-description meta-social-description description
                                      title og-title meta-title meta-social-title
                                      og-image meta-og-image theme-color
                                      favicon link-image-src link-apple-icon link-apple-startup-image]
                               :as renderable}]
  (u/assoc-some
    renderable
    :title               (u/or-text title meta-title)
    :meta-description    (u/or-text meta-description meta-social-description og-description description)
    :meta-title          (u/or-text meta-title title)
    :theme-color         (u/or-text theme-color "white")
    :link-image-src      (u/or-text link-image-src favicon link-apple-icon)
    :link-apple-icon     (u/or-text link-apple-icon favicon link-image-src)
    :link-apple-startup-image (u/or-text link-apple-startup-image link-image-src favicon)
    ;
    :twitter-title       (u/or-text twitter-title meta-social-title og-title)
    :twitter-image       (u/or-text twitter-image og-image meta-og-image)
    :twitter-description (u/or-text twitter-description meta-social-description meta-description description)
    :twitter-card-type   (u/or-text twitter-card-type "summary")
    ;
    :og-title            (u/or-text og-title meta-social-title meta-title title)
    :og-image            (u/or-text og-image meta-og-image)
    :og-description      (u/or-text og-description meta-social-description meta-description description)))

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


(defn- auto-body [body opt-injection]
  (if (keyword? (first body))
    (if (.startsWith (name (first body)) "body")
      (conj body opt-injection)
      [:body body opt-injection])
    body))

(defn- async-sheets [stylesheet-async]
  (cond
    (nil? stylesheet-async)     nil
    (string? stylesheet-async) (u/make-stylesheet-appender-raw stylesheet-async)
    (coll? stylesheet-async)   (apply concat (map u/make-stylesheet-appender stylesheet-async))
    :else (throw (Exception. "not a collection or a string"))))

(defn- link-image [rel src]
  (if src
    (let [img-type (some->> src (re-find #"\.(jpg|jpeg|png)") second (str "image/"))]
      [:link {:rel (name rel) :type (or img-type "image/png"), :href src}])))


(defn ^String render-page
  "Renders a page with a modern meta set.

   Main parameters
   @param {hash-map} renderable
   @param {vector} renderable.body - data structure for Hiccup to render into HTML of the document's body
   @param {string} renderable.title - content for title tag
   @param {string} renderable.favicon - favicon's url
   @param {string} renderable.script - script name, will be loaded asynchronously
   @param {string} renderable.stylesheet - stylesheet filename, will be plugged into the head, will cause
    browser waiting for download.

   Assets
   @param {string} renderable.stylesheet-async - stylesheet filename, will be loaded asynchronously by script.
   @param {string} renderable.garden-css - data structure for Garden CSS
   @param {string/collection<string>} renderable.stylesheet-inline - stylesheet filename, will be inlined into the head.
   @param {string} renderable.script-sync - script name, will be loaded synchronously
   @param {string} renderable.js-module - entry point for JS modules. If you prefer your scripts to be served as modules
   @param {boolean} renderable.skip-cachebusting? will skip automatic cachebusting if true. Defaults to false.
   @param {string} renderable.on-dom-interactive-js - a js snippet to run once DOM is interactive or ready.

   PWA related
   @param {string} renderable.link-image-src - url to image-src
   @param {string} renderable.link-apple-icon - url to image used for apple-touch-icon link
   @param {string} renderable.link-apple-startup-image - url to image used for apple-touch-startup-image link
   @param {string} renderable.theme-color - theme color for PWA (defaults to white)
   @param {string/boolean} renderable.manifest - truthy value will add a manifest link.
    If a string is passed – it'll be treated as a manifest url. Otherwise '/manifest.json'
    will be specified.
   @param {string/boolean} service-worker - service worker url, defaults to /service-worker.js

   More meta
   @param {string} renderable.lang - when provided will render a meta tag and a document attribute
    for page language.
   @param {string} renderable.meta-title - content for title tag (preferred)
   @param {string} renderable.meta-keywords - content for title tag
   @param {string} renderable.meta-description - meta description
   @param {map}    renderable.meta-props – meta which must be rendered as props
    {'fb:app_id' 123}
   @param {string} renderable.head-tags - data structure to render into HTML of the document's head

   Open Graph meta
   @param {string} renderable.og-title - OpenGraph title
   @param {string} renderable.og-description - OpenGraph description
   @param {string} renderable.og-image - absolute url to image for OpenGraph
   @param {string} renderable.og-type
   @param {string} renderable.og-url - OpenGraph page permalink

   Twitter meta
   @param {string}  renderable.twitter-site - twitter @username. Required for all Twitter meta to render
   @param {string}  renderable.twitter-creator - twitter @username.
   @param {keyword} renderable.twitter-card-type - twitter card type
    one of #{:summary  :summary_large_image :app :player}
   @param {string}  renderable.twitter-description - twitter card description
   @param {string}  renderable.twitter-image - twitter image link. Twitter images are useu
   @param {string}  renderable.twitter-image-alt - twitter image alt"
  [^Map renderable]
  (let [renderable (-> renderable
                       u/default-manifest+icon
                       cb/cache-bust-assets
                       provide-default-props)
        {:keys [body title head-tags
                garden-css
                stylesheet stylesheet-inline stylesheet-async
                script script-sync on-dom-interactive-js
                doc-attrs js-module
                favicon link-image-src link-apple-icon link-apple-startup-image
                lang
                manifest service-worker
                meta-title meta-description meta-keywords theme-color
                livereload-script?]} renderable
        doc-attrs  (u/assoc-some doc-attrs :lang lang)
        title      (or meta-title title)
        inline-css (if garden-css (garden/css garden-css))]
    (str
      "<!DOCTYPE html>"
      "<html " (render-attrs doc-attrs) ">"
      (hiccup/html
        [:head

         [:meta {:charset "utf-8"}]
         (m "viewport", "width=device-width, initial-scale=1, maximum-scale=5")

         ; image / icon links
         (link-image :icon favicon)
         (link-image :image_src link-image-src)
         (link-image :apple-touch-icon link-apple-icon)
         (link-image :apple-touch-startup-image link-apple-startup-image)

         (comment
           ; TODO improve support for apple-touch icon with different sizes
           sizes="72x72"
           sizes="114x114"
           sizes="144x144")

         (if lang
           [:meta {:http-equiv "Content-Language" :content lang}])

         (if manifest
           [:link {:rel "manifest" :href (if (string? manifest) manifest "/manifest.json")}])

         (if livereload-script?
           [:script {:src "//localhost:35729/livereload.js?snipver=2" :async true}])

         (if (seq on-dom-interactive-js)
           (on-dom-interactive-fragment on-dom-interactive-js))

         (if service-worker
           (swl/sw-script2 service-worker))

         (seq head-tags)
         ;
         (render-scripts--async script)
         (render-scripts--sync script-sync)
         (render-js-modules js-module)
         ;
         [:title title]
         (m :description meta-description)
         (m :keywords meta-keywords)
         (m :theme-color theme-color)
         (render-meta-props (:meta-props renderable))
         (twitter-meta renderable)
         (og-meta renderable)

         ; Inline CSS and stylesheets
         (render-stylesheets stylesheet)
         (if inline-css [:style#inline-css--garden inline-css])
         (render-inline-sheets stylesheet-inline)])
      (hiccup/html (auto-body body (async-sheets stylesheet-async)))
      "</html>")))

(defn ^Map respond-page
  "Renders a page and returns basic Ring response map"
  [^Map renderable]
  {:status  200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (render-page renderable)})
