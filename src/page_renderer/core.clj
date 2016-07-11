(ns page-renderer.core
  (:require [garden.core :refer [css]]
            [hiccup.page :refer [html5]]))

(defn twitter-meta [{:keys [twitter-username twitter-card-type twitter-title
                            twitter-description twitter-image twitter-image-alt
                            title og-image og-description] :as renderable}]
  (when twitter-username
  (let [twitter-card-type    (name (or twitter-card-type :summary))
        twitter-title        (or twitter-title title)
        twitter-description  (or twitter-description og-description)
        twitter-image        (or twitter-image og-image)]
  (list
    [:meta {:name "twitter:card" :value twitter-card-type}]
    [:meta {:name "twitter:site" :value twitter-username}]
    (if twitter-description
      [:meta {:name "twitter:description" :value twitter-description}])
    (if twitter-image
      [:meta {:name "twitter:image" :value twitter-image}])
    (if twitter-image-alt
      [:meta {:name "twitter:image:alt" :value twitter-username}])
    ))))

(defn og-meta [{:keys [og-image og-title og-description og-url
                       title] :as renderable}]
  (let [og-title (or og-title title)]
  (list
    [:meta {:name "og:title" :value og-title}]
    (if og-url
      [:meta {:name "og:url" :value og-url}])
    (if og-description
      [:meta {:name "og:description" :value og-description}])
    (if og-image [:meta {:name "og:image" :value og-image}]))))

(defn render-page
  "Render a page
   @param {hash-map} renderable
   @param {vector} renderable.body - data structure for Hiccup to render into HTML of the document's body
   @param {string} renderable.title - content for title tag

   @param {string} renderable.og-title - OpenGraph title
   @param {string} renderable.og-description - OpenGraph description
   @param {string} renderable.og-image - absolute url to image for OpenGraph
   @param {string} renderable.og-url - OpenGraph page permalink

   @param {string} renderable.twitter-username - twitter @username
   @param {keyword} renderable.twitter-card-type - twitter card type
    [:summary (default), :summary_large_image, :app, :player]
   @param {string} renderable.twitter-description - twitter card description
   @param {string} renderable.twitter-image - twitter image
   @param {string} renderable.twitter-image-alt - twitter image alt

   @param {string} renderable.garden-css - data structure for Garden CSS
   @param {string} renderable.stylesheet - stylesheet filename
   @param {string} renderable.script - script name
   @param {string} renderable.head-tags - data structure to render into HTML of the document's head"
  [{:keys [body title head-tags stylesheet script og-image garden-css
           twitter-username twitter-card-type] :as renderable}]
  (let [analytics (get renderable :analytics true)
        inline-css (if garden-css (css garden-css))]
  (html5
    [:head
      [:meta {:charset "utf-8"}]
      [:link {:rel "icon", :type "image/png", :href "/favicon.png"}]
      [:meta {:name "viewport", :content "width=device-width, initial-scale=1, maximum-scale=1"}]
      head-tags
      (if inline-css [:style inline-css])
      (if script [:script {:src script, :async true}])
      [:title title]
      (twitter-meta renderable)
      (og-meta renderable)
      (if stylesheet
         [:link {:rel "stylesheet" :type "text/css" :href stylesheet}])]
    body)))

(defn respond-page
  "Renders a page and returns basic Ring response map"
  [renderable]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (render-page renderable)})
