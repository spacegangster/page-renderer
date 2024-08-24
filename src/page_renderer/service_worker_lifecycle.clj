(ns page-renderer.service-worker-lifecycle
  (:require [page-renderer.util :as u]))

(defn sw-script [script-url]
  (u/compile-template
"<script type=\"module\">

import { Workbox } from 'https://storage.googleapis.com/workbox-cdn/releases/5.1.3/workbox-window.prod.mjs';

const promptStr = 'New version of the application is downloaded, do you want to update? May take two reloads.';
function createUIPrompt(opts) {
    if (confirm(promptStr)) {
        opts.onAccept()
    }
}

if ('serviceWorker' in navigator) {
    const wb = new Workbox('${service-worker-url}');

    wb.addEventListener('error', (error) => {
        console.error('Service Worker registration failed:', error);
    });

    wb.addEventListener('waiting', (event) => {
        const prompt = createUIPrompt({
            onAccept: async () => {
                wb.addEventListener('activated', (event) => {
                    console.log('sw-init: activated')
                })
                wb.addEventListener('controlling', (event) => {
                    console.log('sw-init: controlling')
                    window.location.reload();
                });
                wb.messageSW({type: 'SKIP_WAITING'});
            }
        })
    });

    wb.register();
}
</script>"
  {:service-worker-url script-url}))


(defn sw-script-v6 [script-url]
  (u/compile-template
"<script type=\"module\">

import { Workbox } from 'https://storage.googleapis.com/workbox-cdn/releases/6.5.4/workbox-window.prod.mjs';

const promptStr = 'New version of the application is downloaded, do you want to update? May take two reloads.';
function createUIPrompt(opts) {
    if (confirm(promptStr)) {
        opts.onAccept()
    }
}

if ('serviceWorker' in navigator) {
    const wb = new Workbox('${service-worker-url}');

    wb.addEventListener('error', (error) => {
        console.error('Service Worker registration failed:', error);
    });

    wb.addEventListener('waiting', (event) => {
        const prompt = createUIPrompt({
            onAccept: async () => {
                wb.addEventListener('activated', (event) => {
                    console.log('sw-init: activated')
                })
                wb.addEventListener('controlling', (event) => {
                    console.log('sw-init: controlling')
                    window.location.reload();
                });
                wb.messageSW({type: 'SKIP_WAITING'});
            }
        })
    });

    wb.register();
}
</script>"
  {:service-worker-url script-url}))



