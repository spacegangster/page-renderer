(ns page-renderer.service-worker-generator-test
  (:require [clojure.test :refer :all]
            [clojure.string :as s]
            [page-renderer.service-worker-generator :as swg]))


(def sw-params
  {:sw-default-url "/app"
   :sw-add-assets ["/fonts/icomoon.woff"]
   :stylesheet-async "/heavy-stuff.css"
   :script "/lightpad/compiled/app.js"})

(defn- slash-revision [script-str]
  (s/replace script-str #"revision: '[a-z\d]+'" "revision: 'file-hash'"))

(def ethalon-src "test/page_renderer/ethalon-sw.js")

(def ethalon (slurp ethalon-src))

(deftest test-service-worker-generator
  (testing "Sanity test"
    (is (= ethalon (slash-revision (swg/generate-script sw-params))))))

; (run-tests 'page-renderer.service-worker-generator-test)

(comment
  (spit ethalon-src (slash-revision (swg/generate-script sw-params))))
