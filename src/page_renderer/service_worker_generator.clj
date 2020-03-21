(ns page-renderer.service-worker-generator
  (:require [clojure.string :as s]
            [page-renderer.cachebusting :as fu]
            [page-renderer.util :as u])
  (:import (java.util Map)))

(def ^:private template
 "importScripts('https://storage.googleapis.com/workbox-cdn/releases/4.3.1/workbox-sw.js')

workbox.precaching.precacheAndRoute([
    ${precache-assets}
], { ignoreURLParametersMatching: [/hash/] })

workbox.routing.registerNavigationRoute(
    workbox.precaching.getCacheKeyForURL('${default-url}'), {
        whitelist: [ /${whitelist-regex}/ ],
        blacklist: [ /${blacklist-regex}/ ]
    }
)

workbox.routing.setCatchHandler(({event}) => {
    console.log('swm: event ', event)
})

self.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'SKIP_WAITING') {
        console.log('swm: message-received, skipping wait')
        self.skipWaiting()
        console.log('swm: message-received, skipped wait')
    }
})

self.addEventListener('activate', (event) => {
    console.log('swm: activated, claiming clients')
    event.waitUntil( clients.claim() )
    console.log('swm: activated, clients claimed')
})

self.addEventListener('install', () => {
    console.log('swm: installed, skipping wait')
    self.skipWaiting()
    console.log('swm: installed, skipped waiting')
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



(defn ^String generate-script [^Map renderable]
  (let [renderable (u/default-manifest+icon renderable)
        default-url ^String (:sw-default-url renderable "/")
        whitelist-regex (str "^\\\\" default-url)
        blacklist-regex
        (str
          "^\\\\"
          default-url
          (if-not (.endsWith default-url "/") "\\\\/")
          "service-worker\\\\.js")
        sw-assets-to-precache
        (->> (select-keys renderable asset-kws)
             vals
             flatten
             (filter identity)
             set)
        sw-assets-with-revision (map with-revision sw-assets-to-precache)
        sw-assets-str (s/join ",\n    " (map sw-asset-object sw-assets-with-revision))]
    (u/compile-template template {:precache-assets sw-assets-str
                                  :blacklist-regex   blacklist-regex
                                  :whitelist-regex   whitelist-regex
                                  :default-url     default-url})))


(defn ^Map generate-ring-response [^Map renderable]
  {:body    (generate-script renderable)
   :headers {"Content-Type" "text/javascript; charset=utf-8"}
   :status  200})
