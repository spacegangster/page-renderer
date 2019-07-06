(ns page-renderer.core-test
  (:require [clojure.test :refer :all]
            [page-renderer.core :as pr]
            [clojure.string :as s]))

(def test-renderable
  {:title "title"
   :body [:body.quito]})

(def page-2
  {:title "Page"
   :og-image "https://birds.org/great-tit.png"
   :description "Some bird stuff"
   :twitter-site "birds.org"
   :garden-css ; critical path css
    [:h1 {:font-size :20px}]
   :stylesheet-async "large-stuff.css"
   :body [:body.page [:h1 "Ah, a Page!"]]})

(def ethalon-page
  "<!DOCTYPE html><html ><head><meta charset=\"utf-8\" /><link href=\"/favicon.png\" rel=\"icon\" type=\"image/png\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\"><title>title</title><meta property=\"og:title\" content=\"title\"></head><body class=\"quito\"></body></html>")

(def ethalon-page-2
"<!DOCTYPE html><html ><head><meta charset=\"utf-8\" /><link href=\"/favicon.png\" rel=\"icon\" type=\"image/png\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\"><title>Page</title><meta name=\"description\" content=\"Some bird stuff\"><meta name=\"twitter:card\" content=\"summary\"><meta name=\"twitter:site\" content=\"birds.org\"><meta name=\"twitter:description\" content=\"Some bird stuff\"><meta name=\"twitter:image\" content=\"https://birds.org/great-tit.png?mtime=1560282327821\"><meta property=\"og:title\" content=\"Page\"><meta property=\"og:description\" content=\"Some bird stuff\"><meta property=\"og:image\" content=\"https://birds.org/great-tit.png?mtime=1560282327821\"><style id=\"inline-css--garden\">h1 {
  font-size: 20px;
}</style></head><body class=\"page\"><h1>Ah, a Page!</h1><script>(function(){
var link = document.createElement('link');
link.rel='stylesheet';
link.href='large-stuff.css';
link.type='text/css';
document.head.appendChild(link);
})()</script></body></html>")



(deftest sanity
  (testing "Sanity test"
    (is (= ethalon-page
           (pr/render-page test-renderable)))))

(deftest sanity-2
  (testing "Sanity test #2"
    (is (= (s/replace ethalon-page-2 #"mtime=\d+" "mtime=stub")
           (s/replace (pr/render-page page-2)  #"mtime=\d+" "mtime=stub")))))

(deftest manifest-presence
  (testing "Manifest test"
    (let [res (pr/render-page (assoc page-2 :manifest true))]
      (println res)
      (is (or
            (re-find #"<link href=\"/manifest.json\" rel=\"manifest\".?.?>" res)
            (re-find #"<link rel=\"manifest\" href=\"/manifest.json\".?.?>" res))))))


(run-tests 'page-renderer.core-test)
