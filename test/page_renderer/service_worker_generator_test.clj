(ns page-renderer.service-worker-generator-test
  (:require [clojure.test :refer :all]
            [clojure.string :as s]
            [page-renderer.service-worker-generator :as swg]))



(deftest test-service-worker-generator
  (testing "Sanity test"
    (is (swg/generate
          {:sw-default-url "/app"
           :sw-add-assets ["/fonts/icomoon.woff"]
           :stylesheet-async "/heavy-stuff.css"
           :script "/lightpad/compiled/app.js"}))))

