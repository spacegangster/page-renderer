(ns page-renderer.service-worker-generator-test
  (:require [clojure.test :refer :all]
            [clojure.string :as s]
            [page-renderer.service-worker-generator :as swg]))


(def sw-params
  {:sw-default-url   "/app"
   :sw-add-assets    ["/fonts/icomoon.woff"]
   :stylesheet-async "/heavy-stuff.css"
   :script           "/lightpad/compiled/app.js"})

(def sw-params-2
  {:sw-default-url "/"
   :sw-add-assets  ["/fonts/icomoon.woff"]
   :script         "/lightpad/compiled/app.js"})

(defn- slash-revision [script-str]
  (s/replace script-str #"revision: '[a-z\d]+'" "revision: 'file-hash'"))

(def expected-src "test/page_renderer/ethalon-sw.js")
(def expected-2-src "test/page_renderer/ethalon-sw-2.js")

(def expected-str (slurp expected-src))
(def expected-str-2 (slurp expected-2-src))

(deftest test-service-worker-generator
  (testing "Sanity test"
    (is (= expected-str (slash-revision (swg/generate-script sw-params)))))
  (testing "Sanity test for root url"
    (is (= expected-str-2 (slash-revision (swg/generate-script sw-params-2))))))

(comment
  (run-tests 'page-renderer.service-worker-generator-test)
  (spit expected-src (slash-revision (swg/generate-script sw-params)))
  (spit expected-2-src (slash-revision (swg/generate-script sw-params-2))))
