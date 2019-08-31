# page-renderer

##### End-user-ready HTML pages and service workers with social meta, async assets bindings and PWA features

[![Clojars Project](https://img.shields.io/clojars/v/page-renderer.svg)](https://clojars.org/page-renderer)
[![CircleCI](https://circleci.com/gh/spacegangster/page-renderer.svg?style=svg)](https://circleci.com/gh/spacegangster/page-renderer)


## Features
Out of the box:
- Meta for SEO, Twitter, Facebook (Open Graph), link sharing
- Precaching Service Worker generation based on [Workbox](https://developers.google.com/web/tools/workbox/)
- Clojure stylesheets with `garden`
- Clojure markup rendered with `hiccup`
- Built-in cache-busting for assets
- Async stylesheets loading
- And also: that responsive `viewport=something` meta tag and language tag


## Requirements
- Java 8 or later. Java 9 allows better cache-busting with real content hashing.

## Usage

### 1. Define a page
```clojure
(ns pages.home)

(defn page [req]
   ; essentials
  {:title "Lightpad"
   :body [:body.page [:h1 "Ah, a Page!"]]
   :head-tags [[:meta {:name "custom" :property "stuff"}]]
   :stylesheet-async "large-stuff.css" ; injects an async renderer(s)
   :script "/app.js" ; async by default
   :garden-css [:h1 {:font-size :20px}] ; critical path css

   ; seo and meta
   :description "Like a notepad but cyberpunk"
   :og-image "https://lightpad.ai/favicon.png"
   :twitter-site "@lightpad_ai"

   ; PWA stuff
   :manifest    true
   :lang        "en"
   :theme-color "hsl(0, 0%, 96%)"
   :service-worker "/service-worker.js" ; will inject also a service worker lifecycle script
   :sw-default-url "/app"
   :sw-add-assets ["/icons/fonts/icomoon.woff", "/lightning-150.png"]})
```

### 2. Wire it up to your routes (e.g. Compojure)
```clojure
(ns server
 (:require [page-renderer.api :as pr]
           [compojure.core :refer [defroutes GET]] 
           [pages.home :as p]))

(defroutes
  (GET "/" req
   {:status 200
    :headers {"Content-Type" "text/html"}
    :body (pr/render-page (p/page req)})

  (GET "/service-worker.js" req
   {:status 200
    :headers {"Content-Type" "text/javascript"}
    ; will generate a simple Workbox-based service worker on the fly with cache-busting
    :body (pr/generate-service-worker (p/page req))})

  (GET "/quicker-way" req (pr/respond-page (p/page req))))
```


### 3. Celebrate

##### Page output
```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <link href="/favicon.png" rel="icon" type="image/png">
    <meta content="width=device-width, initial-scale=1, maximum-scale=1" name="viewport">
    <title>Page</title>
    <meta content="Some bird stuff" name="description">
    <meta content="summary" name="twitter:card">
    <meta content="birds.org" name="twitter:site">
    <meta content="Some bird stuff" name="twitter:description">
    <meta content="https://birds.org/great-tit.png?mtime=1560280129605" name="twitter:image">
    <meta content="Page" property="og:title">
    <meta content="Some bird stuff" property="og:description">
    <meta content="https://birds.org/great-tit.png?mtime=1560280129605" property="og:image">
    <style id="inline-css--garden">
    h1 {
    font-size: 20px;
    }
    </style>
    
    <!-- Service Worker Lifecycle Snippet -->
    <script>
    import { Workbox } from 'https://storage.googleapis.com/workbox-cdn/releases/4.1.0/workbox-window.prod.mjs';
    
    const promptStr = 'New version of the application is downloaded, do you want to update? May take two reloads.';
    function createUIPrompt(opts) {
      if (confirm(promptStr)) {
         opts.onAccept()
      }
    }
    
    if ('serviceWorker' in navigator) {
      const wb = new Workbox('/service-worker.js');
      wb.addEventListener('waiting', (event) => {
        const prompt = createUIPrompt({
          onAccept: async () => {
            wb.addEventListener('activated', (event) => {
              console.log('sw-init: activated')
              window.location.reload();
            })
            wb.addEventListener('controlling', (event) => {
              console.log('sw-init: controlling')
            });
            wb.messageSW({type: 'SKIP_WAITING'});
          }
        })
      });
      wb.register();
    }
    </script>
</head>
<body class="page">
    <h1>Ah, a Page!</h1>
    <script>
    (function(){
    var link = document.createElement('link');
    link.rel='stylesheet';
    link.href='large-stuff.css';
    link.type='text/css';
    document.head.appendChild(link);
    })()
    </script>
</body>
</html>
```

##### Service Worker
```js
importScripts('https://storage.googleapis.com/workbox-cdn/releases/4.3.1/workbox-sw.js')

workbox.precaching.precacheAndRoute([
    { url: '/heavy-stuff.css', revision: 'file-hash' },
    { url: '/fonts/icomoon.woff', revision: 'file-hash' },
    { url: '/lightpad/compiled/app.js', revision: 'file-hash' },
    { url: '/favicon.png', revision: 'file-hash' },
    { url: '/app', revision: 'file-hash' }
], { ignoreURLParametersMatching: [/hash/] })

workbox.routing.registerNavigationRoute(
    workbox.precaching.getCacheKeyForURL('/app'), {
        whitelist: [ /^\/app/ ],
        blacklist: [ /^\/app\/service-worker.js/ ]
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
})
```


## API

Use `page-renderer.api` namespace.

```
(defn ^String render-page [renderable])
```
Produces an html string.

```
(defn ^String generate-service-worker [renderable])
```
Produces a JavaScript ServiceWorker script text. Service worker will additionally
load [Workbox](https://developers.google.com/web/tools/workbox/) script.


```
(defn ^Map respond-page [renderable])
```
Produces Ring compatible response map with status 200.

```
(defn ^Map respond-service-worker [^Map renderable])
```
Produces Ring compatible response map with status 200.

`renderable` – is a map that may have the following fields

##### Mains

- `@param {hash-map} renderable` - the props map 
- `@param {vector} renderable.body` - data structure for Hiccup to render into HTML of the document's body
- `@param {string} renderable.meta-title` - content for title tag (preferred)
- `@param {string} renderable.title` - content for title tag
- `@param {string} renderable.meta-keywords` - content for title tag
- `@param {string} renderable.meta-description` - meta description
- `@param {map} meta-props` – meta which must be rendered as props.
    Example `{"fb:app_id" 123}`.
    For instance, Facebook `app_id` must be renderded as meta property not just meta tag.

##### Open Graph meta
- `@param {string} renderable.og-title` - OpenGraph title
- `@param {string} renderable.og-description` - OpenGraph description
- `@param {string} renderable.og-image` - absolute url to image for OpenGraph
- `@param {string} renderable.og-type`
- `@param {string} renderable.og-url` - OpenGraph page permalink
- `@param {string} renderable.head-tags` - data structure to render into HTML of the document's head

##### Twitter meta
Twitter meta – if you want it – be sure to include `:twitter-site` or `:twitter-creator`. Or both.

- `@param {string}  renderable.twitter-site` - twitter @username
- `@param {keyword} renderable.twitter-card-type` - twitter card type
    one of `#{:summary  :summary_large_image :app :player}`
- `@param {string}  renderable.twitter-description` - twitter card description
- `@param {string}  renderable.twitter-image` - twitter image
- `@param {string}  renderable.twitter-image-alt` - twitter image alt

##### PWA and Service Worker
- `@param {string} renderable.link-image-src` - url to image-src
- `@param {string} renderable.link-apple-icon` - url to image used for apple-touch-icon link
- `@param {string} renderable.link-apple-startup-image` - url to image used for apple-touch-startup-image link
- `@param {string} renderable.theme-color` - theme color for PWA (defaults to white)
- `@param {string/boolean} renderable.manifest` - truthy value will add a manifest link.
   If a string is passed – it'll be treated as a manifest url. Otherwise '/manifest.json'
   will be specified.
- `@param {string/boolean} service-worker` - service worker url, defaults to /service-worker.js
- `@param {string} renderable.sw-default-url` – application default url.
   Must be an absolute path like '/app'. Defaults to '/'. Will be used in a regexp.
- `@param {collection<string>} renderable.sw-add-assets` - a collection of additional
   assets you want to precache, like ["/fonts/icon-font.woff" "/logo.png"]

##### Assets
- `@param {string} renderable.garden-css` - data structure for Garden CSS
- `@param {string/boolean} renderable.manifest` - truthy value will add a manifest link.
    If a string is passed – it'll be treated as a manifest url.
    Otherwise '/manifest.json' will be specified.
- `@param {string/collection<string>} renderable.stylesheet` - stylesheet filename, will be plugged into the head, will cause
browser waiting for download.
- `@param {string/collection<string>} renderable.stylesheet-inline` - stylesheet filename, will be inlined into the head.
- `@param {string/collection<string>} renderable.stylesheet-async` - stylesheet filename, will be loaded asynchronously by script.
- `@param {string/collection<string>} renderable.script` - script name, will be loaded asynchronously
- `@param {string/collection<string>} renderable.script-sync` - script name, will be loaded synchronously
- `@param {string/collection<string>} renderable.js-module` - entry point for JS modular app. If you prefer your scripts to be served as modules

## Service Worker generation
`page-renderer` allows you to produce a full-blown offline-ready
 [PWA](https://developers.google.com/web/progressive-web-apps/) fast.
Your users will be able to "install" it as a PWA app on mobile platforms or as Chrome
app on desktop platforms. All you need to do is just add another route to your scheme.

### How it works
If you use `service-worker` field then `page-renderer` will generate
a precaching service worker. The worker utilizes
 [Workbox (by Google)](https://developers.google.com/web/tools/workbox/)
and will precache all the assets that you've defined in `renderable`, and will be able
to serve them offline. It also does proper cache-busting with hashes.
`page-renderer` will also inject a service worker lifecycle management script into
your page so that your users will be prompted to download a newer version of your
website when it's ready.

## How cache-busting works here
`page-renderer` provides very basic, but bulletproof cache-busting by providing
a url param with content-hash (or last modification timestamp), like `/file?hash=abec112221122`.
For every stylesheet, script and image on resource paths – it will generate
a content hash. If the file can't be found on the classpath
or inside a local `resources/public` directory it will receive the library load time,
roughly equaling the application start time.


## Where to see in action:

Currently I use it for all my website projects including:
- [Lightpad.ai](https://lightpad.ai) – includes generated service worker, installable PWA
- [Spacegangster.io](https://spacegangster.io) – my website


## License

Copyright © 2019 Ivan Fedorov

Distributed under the MIT License.
