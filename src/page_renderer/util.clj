(ns page-renderer.util)

(defn make-stylesheet-appender [stylesheet-path]
[:script
(str
"(function(){
var link = document.createElement('link');
link.rel='stylesheet';
link.href='" stylesheet-path "';
link.type='text/css';
document.head.appendChild(link);
})()")])
