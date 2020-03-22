(ns page-renderer.api
  (:gen-class)
  (:require [page-renderer.core :as core]
            [page-renderer.service-worker-generator :as swg]
            [page-renderer.cachebusting :as fu])
  (:import (java.util Map)))


(def cache-bust fu/cache-bust)

(defn render-page
  "Renders a page with a modern meta set.

   ^Map renderable:

   Main parameters
   ^Vector :body       - a vector for Hiccup to render into HTML of the document's body
   ^String :title      - content for title tag
   ^String :favicon    - favicon's url
   ^String :script     - script name, will be loaded asynchronously
   ^String :stylesheet - stylesheet filename, will be plugged into the head, will cause
                         browser waiting for download.

   Assets
   ^String  :stylesheet-async - stylesheet filename, will be loaded asynchronously by script.
   ^String  :garden-css       - data structure for Garden CSS
   ^String  :script-sync      - script name, will be loaded synchronously
   ^String  :js-module        - entry point for JS modules. If you prefer your scripts to be served as modules
   ^Boolean :skip-cachebusting?    - will skip automatic cachebusting if set. Defaults to false.
   ^String  :on-dom-interactive-js - a js snippet to run once DOM is interactive or ready.
   ^String/Collection<String> :stylesheet-inline - stylesheet filename, will be inlined into the head.

   PWA related
   ^String  :link-image-src           - url to image-src
   ^String  :link-apple-icon          - url to image used for apple-touch-icon link
   ^String  :link-apple-startup-image - url to image used for apple-touch-startup-image link
   ^String  :theme-color              - theme color for PWA (defaults to white)
   ^String/Boolean :manifest          - truthy value will add a manifest link
      If a string is passed – it'll be treated as a manifest url. Otherwise '/manifest.json'
      will be specified.

   ^String/Boolean :service-worker - service worker url, defaults to /service-worker.js
   ^String         :sw-default-url – application default url.
      Must be an absolute path like '/app'. Defaults to '/'. Will be used in a regexp.
   ^List<String>   :sw-add-assets - a collection of additional
      assets you want to precache, like [\"/fonts/icon-font.woff\" \"/logo.png\"]

   More meta
   ^String :lang             - when provided will render a meta tag and a document attribute for page language.
   ^String :meta-title       - content for the title tag (preferred)
   ^String :meta-keywords    - content for the keywords tag
   ^String :meta-description - meta description
   ^Map    :meta-props       – meta which must be rendered as props {'fb:app_id' 123}
   ^String :head-tags        - data structure to render into HTML of the document's head

   Open Graph meta (@link https://ogp.me)
   ^String :og-title       - OpenGraph title
   ^String :og-description - OpenGraph description
   ^String :og-image       - absolute url to image for OpenGraph
   ^String :og-type        - OpenGraph object type
   ^String :og-url         - OpenGraph page permalink

   Twitter meta (@link https://developer.twitter.com/en/docs/tweets/optimize-with-cards/guides/getting-started)
   ^String  :twitter-site        - Twitter @username. Required for all Twitter meta to render
   ^String  :twitter-creator     - Twitter @username.
   ^Keyword :twitter-card-type   - Twitter card type one of #{:summary  :summary_large_image :app :player}
   ^String  :twitter-description - Twitter card description
   ^String  :twitter-image       - Twitter image link. Twitter images are useu
   ^String  :twitter-image-alt   - Twitter image alt"
  ^String [^Map renderable]
  (core/render-page renderable))

(defn respond-page
  "Renders a page and returns basic Ring response map
   See render-page for docs on renderable."
  ^Map [^Map renderable]
  (core/respond-page renderable))


(defn generate-service-worker
  "Generates a service worker script that will precache all the assets from renderable.
   ^Map renderable:
     ^String/Boolean :service-worker - service worker url, defaults to /service-worker.js
     ^String         :sw-default-url – application default url.
        Must be an absolute path like '/app'. Defaults to '/'. Will be used in a regexp.
     ^List<String>   :sw-add-assets - a collection of additional
        assets you want to precache, like [\"/fonts/icon-font.woff\" \"/logo.png\"]

   See render-page for docs on renderable."
  ^String [^Map renderable]
  (swg/generate-script renderable))

(defn respond-service-worker
  "Generates a Ring response map containing a service worker script as a body.
  See generate-service-worker for more docs"
  ^Map [^Map renderable]
  (swg/generate-ring-response renderable))

