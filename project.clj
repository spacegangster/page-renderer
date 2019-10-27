(defproject page-renderer "0.4.3"
  :description "A page renderer ready for social networks and PWA"
  :url "https://github.com/spacegangster/page-renderer"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [garden              "1.3.9"]
                 [hiccup              "1.0.5"]]
  :main page-renderer.api
  :java-source-paths ["src"]
  :javac-options ["-source" "8" "-target" "8"
                  "-XDignore.symbol.file"
                  "-Xlint:all,-options,-path"
                  "-Werror"
                  "-proc:none"]
  :profiles
  {:debug-resources
   {:source-paths ["src" "dev"]
    :aot :all
    :main user}})
