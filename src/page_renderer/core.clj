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


(defn render-page ^String [^Map renderable]
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

(defn respond-page
  "Renders a page and returns basic Ring response map"
  ^Map [^Map renderable]
  {:status  200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (render-page renderable)})
