# page-renderer

##### HTML pages with proper meta and styles in pure Clojure

## Features
From the box:
- Basic SEO meta
- Basic Twitter meta
- Basic Open Graph (Facebook) meta
- Clojure stylesheets with `garden`
- Clojure markup rendered with `hiccup`
- Built-in cache-busting for assets
- Async stylesheets loading

## Usage

### 1. Define a page
```clojure
(ns pages.home)

(def page
  {:title "Page"
   :og-image "https://birds.org/great-tit.png"
   :description "Some bird stuff"
   :twitter-site "birds.org"
   :garden-css ; critical path css
    [:h1 {:font-size :20px}]
   :stylesheet-async "large-stuff.css"
   :body [:body.page [:h1 "Ah, a Page!"]]})
```

### 2. Wire it up to your routes (e.g. Compojure)
``` clojure
(ns server
 (:require [page-renderer.core :as pr]
           [compojure.core :refer [defroutes GET]] 
           [pages.home :as p]))

(defroutes
  (GET "/" [] {:status 200
               :headers {"Content-Type" "text/html"}
               :body (pr/render-page p/page)})
  (GET "/quicker-way" [] (pr/respond-page p/page)))
```

### 3. Celebrate
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

## API
Use `page-renderer.core/render-page` and `page-renderer.core/respond-page`
For both functions same signature.
```
Render a page
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
@param {string} renderable.head-tags - data structure to render into HTML of the document's head
```

## License

Copyright © 2019 Ivan Fedorov

Distributed under the Eclipse Public License either version 1.0
 or (at your option) any later version.
