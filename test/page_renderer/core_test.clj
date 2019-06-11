(ns page-renderer.core-test
  (:require [clojure.test :refer :all]
            [page-renderer.core :as pr]))

(def test-renderable
  {:title "title"
   :body [:body.quito]})

(def ethalon-page
  "<!DOCTYPE html><html ><head><meta charset=\"utf-8\" /><link href=\"/favicon.png\" rel=\"icon\" type=\"image/png\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\"><title>title</title><meta property=\"og:title\" content=\"title\"></head><body class=\"quito\"></body></html>")


(deftest sanity
  (testing "Sanity test"
    (is (= ethalon-page
           (pr/render-page test-renderable)))))
