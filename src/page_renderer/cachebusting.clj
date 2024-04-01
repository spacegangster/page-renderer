(ns page-renderer.cachebusting
  (:require [clojure.java.io :as io]
            [page-renderer.util :as u]
            [clojure.string :as s])
  (:import (java.security MessageDigest DigestInputStream)
           page_renderer.CacheBustHelper))


(def ^:private default-mtime (System/currentTimeMillis))
(def ^:private default-hash (u/hexify (.getBytes (str default-mtime))))



(defn- try-get-mtime-for-a-file [web-asset-path]
  (let [file (io/file (str "resources/public" web-asset-path))]
    (if (.exists file)
      (.lastModified file)
      default-mtime)))

(def ^:private java-version
  (Integer/parseInt (last (s/split (System/getProperty "java.specification.version") #"\."))))

(def ^:private can-read-all-bytes? (> java-version 8))

(defn hash-resource [r]
  (let [md (MessageDigest/getInstance "MD5")]
    (with-open [is (io/input-stream r)
                dis (DigestInputStream. is md)]
      (if can-read-all-bytes?
        (.readAllBytes dis)
        (CacheBustHelper/readAllBytes dis))
      (u/hexify (.digest md)))))

(comment
  (hash-resource (io/resource "main.css")))


(defn mtime-or-default [web-asset-path]
  (if-let [resource (io/resource (str "public" web-asset-path))]
    (try
      (.. resource openConnection getLastModified)
      (catch Exception e
        default-mtime))
    (try-get-mtime-for-a-file web-asset-path)))


(def ^:dynamic hash-fn hash-resource)

(defn hash-or-default [res-path]
  (let [relative? (not (re-find #"^\/" res-path))
        resource (or (if relative?
                       (io/resource res-path)
                       (io/resource (str "public" res-path)))
                     (io/resource res-path))]
    (if resource
      (try
        (hash-fn resource)
        (catch Exception e
          default-hash))
      default-hash)))

(defn cache-bust-one [web-asset-path]
  (if web-asset-path
    (str web-asset-path "?hash=" (hash-or-default web-asset-path))))

(defn cache-bust ^String [^String web-asset-path-or-coll]
  (if (coll? web-asset-path-or-coll)
    (map cache-bust-one web-asset-path-or-coll)
    (cache-bust-one web-asset-path-or-coll)))

(defn cache-bust-assets [page-data]
  (if (:skip-cachebusting? page-data)
    page-data
    (-> page-data
        (u/update-if-present :twitter-image cache-bust-one)
        (u/update-if-present :og-image cache-bust-one)
        (u/update-if-present :favicon cache-bust-one)
        (u/update-if-present :link-apple-icon cache-bust-one)
        (u/update-if-present :link-apple-startup-image cache-bust-one)
        (u/update-if-present :link-image-src cache-bust-one)
        ;
        (u/update-if-present :script cache-bust)
        (u/update-if-present :script-sync cache-bust)
        (u/update-if-present :js-module cache-bust)
        ;
        (u/update-if-present :stylesheet cache-bust)
        (u/update-if-present :stylesheet-async cache-bust)
        ;
        (u/update-if-present :manifest cache-bust-one))))
