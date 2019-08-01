(ns page-renderer.api
  (:require [page-renderer.core :as core]
            [page-renderer.service-worker-generator :as swg]
            [page-renderer.cachebusting :as fu]))

(def cache-bust fu/cache-bust)

(def render-page core/render-page)

(def respond-page core/respond-page)

(def generate-service-worker swg/generate)
