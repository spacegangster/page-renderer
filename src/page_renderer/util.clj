(ns page-renderer.util
  (:require [clojure.string :as s]))

(defn update-if-present [m k f]
  (if (contains? m k)
    (update m k f)
    m))

(defn or-text [& texts]
  (first (filter seq texts)))

(defn assoc-some
  ; taken from weavejester/medley
  "Associates a key with a value in a map, if and only if the value is not nil."
  ([m k v]
   (if (nil? v) m (assoc m k v)))
  ([m k v & kvs]
   (reduce (fn [m [k v]] (assoc-some m k v))
           (assoc-some m k v)
           (partition 2 kvs))))


(defn- ^String -str-compiler [template [k v]]
  (let [val-str (if (keyword? v) (name v) (str v))
        key-pattern (re-pattern (str "\\$\\{" (name k) "\\}"))]
    (s/replace template key-pattern val-str)))

(defn ^String compile-template
  "Replaces named slots with values"
  [template-str params]
  (reduce -str-compiler template-str params))

(defn default-manifest+icon [renderable]
  (-> renderable
      (update-if-present :manifest #(if (string? %) % "/manifest.json"))
      (assoc :favicon (or-text (:favicon renderable) "/favicon.png"))))

;(compile-template "Go ${joel}!" {:joel "hard, Joel"})

(defn ^String hexify
  "Convert byte sequence to hex string
  kudos to Grzegorz Luczywo, https://stackoverflow.com/a/15627016/1273651"
  [coll]
  (let [hex [\0 \1 \2 \3 \4 \5 \6 \7 \8 \9 \a \b \c \d \e \f]]
    (letfn [(hexify-byte [b]
              (let [v (bit-and b 0xFF)]
                [(hex (bit-shift-right v 4)) (hex (bit-and v 0x0F))]))]
      (apply str (mapcat hexify-byte coll)))))


(defn make-stylesheet-appender-raw [stylesheet-path]
[:script
(str
"(function(){
var link = document.createElement('link');
link.rel='stylesheet';
link.href='" stylesheet-path "';
link.type='text/css';
document.head.appendChild(link);
})()")])

(defn make-stylesheet-appender [stylesheet-path]
  (list (make-stylesheet-appender-raw stylesheet-path)
        [:noscript [:link {:rel "stylesheet" :type "text/css" :href stylesheet-path}]]))
