(ns page-renderer.core
  (:require [garden.core :refer [css]]
            [hiccup.page :refer [html5]]))

(defn render-page
  "Render a page
   @param {hash-map} renderable
   @param {vector} renderable.body - data structure for Hiccup to render into HTML of the document's body
   @param {string} renderable.title - content for title tag
   @param {string} renderable.garden-css - data structure for Garden CSS
   @param {string} renderable.stylesheet - stylesheet filename
   @param {string} renderable.script - script name
   @param {string} renderable.head-tags - data structure to render into HTML of the document's head"
  [{:keys [body title head-tags stylesheet script og-image garden-css] :as renderable}]
  (let [analytics (get renderable :analytics true)
        inline-css (if garden-css (css garden-css))]
  (html5
    [:head
      site-meta
      head-tags
      (if inline-css [:style inline-css])
      (if script [:script {:src script, :async true}])
      [:title title]
      [:meta {:name "og:title" :value title}]
      (if og-image [:meta {:name "og:image" :value og-image}])
      (if stylesheet
         [:link {:rel "stylesheet" :type "text/css" :href stylesheet}])]
    body)))

(defn respond-page
  "Renders a page and returns basic Ring response map"
  [renderable]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (render-page renderable)})
