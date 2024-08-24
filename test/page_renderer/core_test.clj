(ns page-renderer.core-test
  (:require [clojure.test :refer :all]
            [clojure.string :as s]
            [page-renderer.core :as pr]))

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
   :body [:body.page [:h1 "Ah, a Page!"]]})

(def page-3
  {:title "Page"
   :stylesheet-async ["large-stuff.css" "large-stuff2.css"]
   :body [:body.page [:h1 "Ah, a Page!"]]})

(def page-4
  {:title "Page"
   :stylesheet ["large-stuff.css" "large-stuff2.css"]
   :body [:div.page "a page"]})

(def page-5
  {:title "Page"
   :service-worker "/sw-2.js"
   :body [:div.page "a page"]})



(defn- slash-mtime [page-str]
  (-> page-str
      (s/replace #"mtime=\d+" "mtime=stub")
      (s/replace #"hash=[a-z\d]+" "hash=stub")))

(def ethalon-page
  "<!DOCTYPE html><html ><head><meta charset=\"utf-8\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=5\"><link href=\"/favicon.png?hash=stub\" rel=\"icon\" type=\"image/png\" /><link href=\"/favicon.png?hash=stub\" rel=\"image_src\" type=\"image/png\" /><link href=\"/favicon.png?hash=stub\" rel=\"apple-touch-icon\" type=\"image/png\" /><link href=\"/favicon.png?hash=stub\" rel=\"apple-touch-startup-image\" type=\"image/png\" /><title>title</title><meta name=\"theme-color\" content=\"white\"><meta property=\"og:title\" content=\"title\"></head><body class=\"quito\"></body></html>")

(def ethalon-page-2
  "<!DOCTYPE html><html ><head><meta charset=\"utf-8\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=5\"><link href=\"/favicon.png?hash=stub\" rel=\"icon\" type=\"image/png\" /><link href=\"/favicon.png?hash=stub\" rel=\"image_src\" type=\"image/png\" /><link href=\"/favicon.png?hash=stub\" rel=\"apple-touch-icon\" type=\"image/png\" /><link href=\"/favicon.png?hash=stub\" rel=\"apple-touch-startup-image\" type=\"image/png\" /><title>Page</title><meta name=\"description\" content=\"Some bird stuff\"><meta name=\"theme-color\" content=\"white\"><meta name=\"twitter:card\" content=\"summary\"><meta name=\"twitter:site\" content=\"birds.org\"><meta name=\"twitter:description\" content=\"Some bird stuff\"><meta name=\"twitter:image\" content=\"https://birds.org/great-tit.png?hash=stub\"><meta property=\"og:title\" content=\"Page\"><meta property=\"og:description\" content=\"Some bird stuff\"><meta property=\"og:image\" content=\"https://birds.org/great-tit.png?hash=stub\"><style id=\"inline-css--garden\">h1 {
  font-size: 20px;
}</style></head><body class=\"page\"><h1>Ah, a Page!</h1></body></html>")

(def ethalon-page-3
  "<!DOCTYPE html><html ><head><meta charset=\"utf-8\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=5\"><link href=\"/favicon.png?hash=stub\" rel=\"icon\" type=\"image/png\" /><link href=\"/favicon.png?hash=stub\" rel=\"image_src\" type=\"image/png\" /><link href=\"/favicon.png?hash=stub\" rel=\"apple-touch-icon\" type=\"image/png\" /><link href=\"/favicon.png?hash=stub\" rel=\"apple-touch-startup-image\" type=\"image/png\" /><title>Page</title><meta name=\"theme-color\" content=\"white\"><meta property=\"og:title\" content=\"Page\"></head><body class=\"page\"><h1>Ah, a Page!</h1><script>(function(){
var link = document.createElement('link');
link.rel='stylesheet';
link.href='large-stuff.css?hash=stub';
link.type='text/css';
document.head.appendChild(link);
})()</script><noscript><link href=\"large-stuff.css?hash=stub\" rel=\"stylesheet\" type=\"text/css\" /></noscript><script>(function(){
var link = document.createElement('link');
link.rel='stylesheet';
link.href='large-stuff2.css?hash=stub';
link.type='text/css';
document.head.appendChild(link);
})()</script><noscript><link href=\"large-stuff2.css?hash=stub\" rel=\"stylesheet\" type=\"text/css\" /></noscript></body></html>")

(def ethalon-page-4
  "<!DOCTYPE html><html ><head><meta charset=\"utf-8\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=5\"><link href=\"/favicon.png?hash=stub\" rel=\"icon\" type=\"image/png\" /><link href=\"/favicon.png?hash=stub\" rel=\"image_src\" type=\"image/png\" /><link href=\"/favicon.png?hash=stub\" rel=\"apple-touch-icon\" type=\"image/png\" /><link href=\"/favicon.png?hash=stub\" rel=\"apple-touch-startup-image\" type=\"image/png\" /><title>Page</title><meta name=\"theme-color\" content=\"white\"><meta property=\"og:title\" content=\"Page\"><link href=\"large-stuff.css?hash=stub\" rel=\"stylesheet\" type=\"text/css\" /><link href=\"large-stuff2.css?hash=stub\" rel=\"stylesheet\" type=\"text/css\" /></head><body><div class=\"page\">a page</div></body></html>")


(def ethalon-page-5
  "<!DOCTYPE html><html ><head><meta charset=\"utf-8\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=5\"><link href=\"/favicon.png?hash=stub\" rel=\"icon\" type=\"image/png\" /><link href=\"/favicon.png?hash=stub\" rel=\"image_src\" type=\"image/png\" /><link href=\"/favicon.png?hash=stub\" rel=\"apple-touch-icon\" type=\"image/png\" /><link href=\"/favicon.png?hash=stub\" rel=\"apple-touch-startup-image\" type=\"image/png\" /><script type=\"module\">

import { Workbox } from 'https://storage.googleapis.com/workbox-cdn/releases/6.5.4/workbox-window.prod.mjs';

const promptStr = 'New version of the application is downloaded, do you want to update? May take two reloads.';
function createUIPrompt(opts) {
    if (confirm(promptStr)) {
        opts.onAccept()
    }
}

if ('serviceWorker' in navigator) {
    const wb = new Workbox('/sw-2.js');

    wb.addEventListener('error', (error) => {
        console.error('Service Worker registration failed:', error);
    });

    wb.addEventListener('waiting', (event) => {
        const prompt = createUIPrompt({
            onAccept: async () => {
                wb.addEventListener('activated', (event) => {
                    console.log('sw-init: activated')
                })
                wb.addEventListener('controlling', (event) => {
                    console.log('sw-init: controlling')
                    window.location.reload();
                });
                wb.messageSW({type: 'SKIP_WAITING'});
            }
        })
    });

    wb.register();
}
</script><title>Page</title><meta name=\"theme-color\" content=\"white\"><meta property=\"og:title\" content=\"Page\"></head><body><div class=\"page\">a page</div></body></html>")


(deftest sanity
  (testing "Sanity test"
    (is (= ethalon-page
           (slash-mtime (pr/render-page page-1))))))

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

(deftest service-worker-lifecycle-injection
  (testing "injects lifecycle snippet with a correct url"
    (is (= ethalon-page-5
           (slash-mtime (pr/render-page page-5))))))

(deftest manifest-presence
  (testing "Manifest test"
    (let [res (slash-mtime (pr/render-page (assoc page-1 :manifest true)))]
      (is (or
            (re-find #"<link href=\"/manifest.json\?hash=stub\" rel=\"manifest\".?.?>" res)
            (re-find #"<link rel=\"manifest\" href=\"/manifest.json\?hash=stub\".?.?>" res))))))

(deftest lang-presence
  (testing "Lang presence test"
    (let [res (pr/render-page (assoc page-2 :lang :en))]
      (is (and
            (re-find #"<html.*lang=\"en\".*?>" res)
            (re-find #"<meta.*http-equiv=\"Content-Language\".*?/>" res))))))

(comment
  (run-tests 'page-renderer.core-test))
