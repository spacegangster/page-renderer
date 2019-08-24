(ns user
  (:gen-class)
  (:require [clojure.java.io :as io]
            [page-renderer.api :as api]))


(comment
  (io/resource "main.css")
  (api/cache-bust "main.css"))

(defn -main []
  (println (api/cache-bust "main.css"))
  (println (api/cache-bust "public/main.css")))
