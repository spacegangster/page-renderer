(ns page-renderer.service-worker-generator
  (:require [clojure.string :as s]
            [page-renderer.cachebusting :as fu]
            [page-renderer.util :as u]))

(def ^:private template
"importScripts('https://storage.googleapis.com/workbox-cdn/releases/4.3.1/workbox-sw.js')

workbox.precaching.precacheAndRoute([
    ${precache-assets}
], { ignoreURLParametersMatching: [/hash/] })

workbox.routing.registerNavigationRoute(
    workbox.precaching.getCacheKeyForURL('${default-url}'), {
        whitelist: [ /^\\${default-url}/ ],
        blacklist: [ /^\\${default-url}\\/service-worker.js/ ]
    }
)

workbox.routing.setCatchHandler(({event}) => {
    console.log('swm: event ', event)
})

addEventListener('message', (event) => {
    if (event.data && event.data.type === 'SKIP_WAITING') {
        console.log('swm: skipping waiting')
        skipWaiting()
    }
})

self.addEventListener('activate', () => {
    console.log('swm: activated')
})

self.addEventListener('install', () => {
    console.log('swm: installed')
})")

(def ^:private asset-kws
  [:twitter-image :og-image
   :favicon :link-apple-icon :link-apple-startup-image :link-image-src
   :script :script-sync :js-module
   :sw-default-url :sw-add-assets
   :stylesheet :stylesheet-async
   :manifest])


(defn- with-revision [asset-path]
  [asset-path (fu/hash-or-default asset-path)])

(defn- sw-asset-object [[url revision]]
  (str "{ url: '" url "', revision: '" revision "' }"))



(defn generate-script
  "renderable.sw-default-url {string} – application default url.
    Must be an absolute path like '/app'. Defaults to '/'. Will be used in a regexp.
   renderable.sw-add-assets {collection<string>} - a collection of additional
    assets you want to precache, like [\"/fonts/icon-font.woff\",\"/logo.png\"]"
  [renderable]
  (let [renderable (u/default-manifest+icon renderable)
        sw-assets-to-precache
        (->> (select-keys renderable asset-kws)
             vals
             flatten
             (filter identity)
             set)
        sw-assets-with-revision (map with-revision sw-assets-to-precache)
        sw-assets-str (s/join ",\n    " (map sw-asset-object sw-assets-with-revision))]
    (u/compile-template template {:precache-assets sw-assets-str
                                  :default-url     (:sw-default-url renderable "/")})))


(defn generate-ring-response
  "renderable.sw-default-url {string} – application default url.
    Must be an absolute path like '/app'. Defaults to '/'. Will be used in a regexp.
   renderable.sw-add-assets {collection<string>} - a collection of additional
    assets you want to precache, like [\"/fonts/icon-font.woff\",\"/logo.png\"]"
  [renderable]
  {:body    (generate-script renderable)
   :headers {"Content-Type" "text/javascript; charset=utf-8"}
   :status  200})
