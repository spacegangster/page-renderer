(defproject page-renderer "0.4.8"
  :description
  "A holistic approach for rendering a modern HTML web app frame.
   Has bindings for common page meta, PWA, and social meta tags."

  :url "https://github.com/spacegangster/page-renderer"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.11.2"]]
  :signing {:gpg-key "hello@spacegangster.io"}
  :main page-renderer.api
  :java-source-paths ["src"]
  :javac-options ["-source" "8" "-target" "8"
                  "-XDignore.symbol.file"
                  "-Xlint:all,-options,-path"
                  "-Werror"
                  "-proc:none"]
  :deploy-repositories [["releases" :clojars]]
  :profiles
  {:provided {:dependencies
              [[org.clojure/clojure "1.11.2"]
               [garden "1.3.9"]
               [hiccup "1.0.5"]]}
   :debug-resources
   {:source-paths ["src" "dev"]
    :aot :all
    :main user}})
