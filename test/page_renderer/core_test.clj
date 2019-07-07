(ns page-renderer.core-test
  (:require [clojure.test :refer :all]
            [page-renderer.core :as pr]
            [clojure.string :as s]))

(def page-1
  {:title "title"
   :body  [:body.quito]})

(def page-2
  {:title "Page"
   :og-image "https://birds.org/great-tit.png"
   :description "Some bird stuff"
   :twitter-site "birds.org"
   :garden-css ; critical path css
    [:h1 {:font-size :20px}]
   :stylesheet-async "large-stuff.css"
   :body [:body.page [:h1 "Ah, a Page!"]]})

(def page-3
  {:title "Page"
   :stylesheet-async ["large-stuff.css" "large-stuff2.css"]
   :body [:body.page [:h1 "Ah, a Page!"]]})

(def page-4
  {:title "Page"
   :stylesheet ["large-stuff.css" "large-stuff2.css"]
   :body [:div.page "a page"]})

(defn- slash-mtime [page-str]
  (s/replace page-str #"mtime=\d+" "mtime=stub"))

(def ethalon-page
  (slash-mtime
  "<!DOCTYPE html><html ><head><meta charset=\"utf-8\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\"><link href=\"/favicon.png\" rel=\"icon\" type=\"image/png\" /><title>title</title><meta property=\"og:title\" content=\"title\"></head><body class=\"quito\"></body></html>"))

(def ethalon-page-2
  "<!DOCTYPE html><html ><head><meta charset=\"utf-8\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\"><link href=\"/favicon.png\" rel=\"icon\" type=\"image/png\" /><title>Page</title><meta name=\"description\" content=\"Some bird stuff\"><meta name=\"twitter:card\" content=\"summary\"><meta name=\"twitter:site\" content=\"birds.org\"><meta name=\"twitter:description\" content=\"Some bird stuff\"><meta name=\"twitter:image\" content=\"https://birds.org/great-tit.png?mtime=stub\"><meta property=\"og:title\" content=\"Page\"><meta property=\"og:description\" content=\"Some bird stuff\"><meta property=\"og:image\" content=\"https://birds.org/great-tit.png?mtime=stub\"><style id=\"inline-css--garden\">h1 {\n  font-size: 20px;\n}</style></head><body class=\"page\"><h1>Ah, a Page!</h1><script>(function(){\nvar link = document.createElement('link');\nlink.rel='stylesheet';\nlink.href='large-stuff.css?mtime=stub';\nlink.type='text/css';\ndocument.head.appendChild(link);\n})()</script></body></html>")

(def ethalon-page-3
  "<!DOCTYPE html><html ><head><meta charset=\"utf-8\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\"><link href=\"/favicon.png\" rel=\"icon\" type=\"image/png\" /><title>Page</title><meta property=\"og:title\" content=\"Page\"></head><body class=\"page\"><h1>Ah, a Page!</h1><script>(function(){\nvar link = document.createElement('link');\nlink.rel='stylesheet';\nlink.href='large-stuff.css?mtime=stub';\nlink.type='text/css';\ndocument.head.appendChild(link);\n})()</script><script>(function(){\nvar link = document.createElement('link');\nlink.rel='stylesheet';\nlink.href='large-stuff2.css?mtime=stub';\nlink.type='text/css';\ndocument.head.appendChild(link);\n})()</script></body></html>")

(def ethalon-page-4
  "<!DOCTYPE html><html ><head><meta charset=\"utf-8\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\"><link href=\"/favicon.png\" rel=\"icon\" type=\"image/png\" /><title>Page</title><meta property=\"og:title\" content=\"Page\"><link href=\"large-stuff.css?mtime=stub\" rel=\"stylesheet\" type=\"text/css\" /><link href=\"large-stuff2.css?mtime=stub\" rel=\"stylesheet\" type=\"text/css\" /></head><body><div class=\"page\">a page</div></body></html>")


(deftest sanity
  (testing "Sanity test"
    (is (= ethalon-page
           (pr/render-page page-1)))))

(deftest sanity-2
  (testing "Sanity test #2"
    (is (= ethalon-page-2
           (slash-mtime (pr/render-page page-2))))))

(deftest multiple-async-stylesheets
  (testing "multiple-async-stylesheets"
    (is (= ethalon-page-3
           (slash-mtime (pr/render-page page-3))))))

(deftest stylesheet-cachebusting
  (testing "stylesheet cachebusting"
    (is (= ethalon-page-4
           (slash-mtime (pr/render-page page-4))))))

(deftest manifest-presence
  (testing "Manifest test"
    (let [res (slash-mtime (pr/render-page (assoc page-1 :manifest true)))]
      (is (or
            (re-find #"<link href=\"/manifest.json\?mtime=stub\" rel=\"manifest\".?.?>" res)
            (re-find #"<link rel=\"manifest\" href=\"/manifest.json\?mtime=stub\".?.?>" res))))))

(deftest lang-presence
  (testing "Lang presence test"
    (let [res (pr/render-page (assoc page-2 :lang :en))]
      (is (and
            (re-find #"<html.*lang=\"en\".*?>" res)
            (re-find #"<meta.*http-equiv=\"Content-Language\".*?/>" res)
            )))))


;(run-tests 'page-renderer.core-test)
