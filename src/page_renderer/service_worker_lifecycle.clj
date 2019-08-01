(ns page-renderer.service-worker-lifecycle
  (:require [page-renderer.cachebusting :as fu]
            [page-renderer.util :as u]))

(defn sw-script2 [script-url]
  (u/compile-template
"<script type=\"module\">

import { Workbox } from 'https://storage.googleapis.com/workbox-cdn/releases/4.1.0/workbox-window.prod.mjs';

const promptStr = 'New version of the application is downloaded, do you want to update? May take two reloads.';
function createUIPrompt(opts) {
  if (confirm(promptStr)) {
     opts.onAccept()
  }
}

if ('serviceWorker' in navigator) {
  const wb = new Workbox('${service-worker-url}');
  wb.addEventListener('waiting', (event) => {
    const prompt = createUIPrompt({
      onAccept: async () => {
        wb.addEventListener('activated', (event) => {
          console.log('sw-init: activated')
          window.location.reload();
        })
        wb.addEventListener('controlling', (event) => {
          console.log('sw-init: controlling')
        });
        wb.messageSW({type: 'SKIP_WAITING'});
      }
    })
  });
  wb.register();
}
</script>"
  {:service-worker-url script-url}))
