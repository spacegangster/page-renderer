# page-renderer

A Clojure library designed to render HTML pages with styles using only Clojure.
Simplify your workflow with page rendering -- no more external preprocessors,
no more manual work to set page titles and metadata. Want a quick way to render
a modern fancy page? Just write it in plain Clojure, starting with one namespace.
Built with top-notch [Hiccup (HTML rendering)](https://github.com/weavejester/hiccup) and
[Garden (CSS generator)](https://github.com/noprompt/garden) libraries Page-Renderer
enables you to do simple things quickly. See usage.

## Features
- Clojure-only option for HTML and CSS rendering
- Support for Open Graph and Twitter metadata
- Built-in cachebusting for external js and css files

## Usage

### 1. Define a page
```clojure
(ns my-page)

(def page
  {:title "French baguette"
   :garden-css
    [:h1 {:font-size :20px}]
   :body
    [:body.page
     [:h1 "Ah, my beloved baguettes!"]})
```
Here the page is defined via plain def, however you can use a `defn` to
generate page dynamically.

### 2. Wire it up to your routes (e.g. Compojure)
``` clojure
(ns server
 (:require [page-renderer.core :refer [render-page respond-page]]))

(defroutes
  (GET "/" [] {:status 200
               :headers {"Content-Type" "text/html"}
               :body (render-page my-page)})
  (GET "/quicker-way" [] (respond-page my-page)))
```

### 3. Celebrate


## License

Copyright © 2016 Ivan Fedorov

Distributed under the Eclipse Public License, the same as Clojure.
your option) any later version.
