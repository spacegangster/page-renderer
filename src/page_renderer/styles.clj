(ns page-renderer.styles
  (:require [clojure.string :as s]
            [garden.stylesheet :refer [at-media]]))

(defn bem-str [css-class-name & modifiers]
  (let [base-class-name (name css-class-name)
        -append
        (fn [final-class-name modifier-name]
          (if modifier-name
            (str  final-class-name  " "  base-class-name "--" (name modifier-name))
            final-class-name))]
    (reduce -append base-class-name (flatten modifiers))))

(defn bem [base-class & modifiers]
  {:class (apply bem-str (cons base-class modifiers))})

(defn at-desktop [& rules]
  (at-media {:min-width :768px} rules))

(defn at-mobile [& rules]
  (at-media {:max-width :767px} rules))

(def mini-reset
  ;Mini-reset + responsive images and video"
  [["html,body,div,p,article,section,h1,h2,h3,h4,h5,h6,figure,figcaption,header,footer,span,ul,ol,li"
     {:margin 0
      :padding 0
      :box-sizing :border-box}]
   [:.hidden {:display :none}]
   [:.nolink
    {:text-decoration :none
     :color :inherit}]
   ["img, image, video"
     {:max-width :100%
      :object-fit :cover}]
   [:iframe
     {:max-width :100%}]])
