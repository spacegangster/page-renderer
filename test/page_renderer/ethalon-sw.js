importScripts('https://storage.googleapis.com/workbox-cdn/releases/4.3.1/workbox-sw.js')

workbox.precaching.precacheAndRoute([
    { url: '/heavy-stuff.css', revision: 'file-hash' },
    { url: '/fonts/icomoon.woff', revision: 'file-hash' },
    { url: '/lightpad/compiled/app.js', revision: 'file-hash' },
    { url: '/favicon.png', revision: 'file-hash' },
    { url: '/app', revision: 'file-hash' }
], { ignoreURLParametersMatching: [/hash/] })

workbox.routing.registerNavigationRoute(
    workbox.precaching.getCacheKeyForURL('/app'), {
        whitelist: [ /^\/app/ ],
        blacklist: [ /^\/app\/service-worker.js/ ]
    }
)

workbox.routing.setCatchHandler(({event}) => {
    console.log('swm: event ', event)
})

self.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'SKIP_WAITING') {
        console.log('swm: message-received, skipping wait')
        self.skipWaiting()
        console.log('swm: message-received, skipped wait')
    }
})

self.addEventListener('activate', (event) => {
    console.log('swm: activated, claiming clients')
    event.waitUntil( clients.claim() )
    console.log('swm: activated, clients claimed')
})

self.addEventListener('install', () => {
    console.log('swm: installed, skipping wait')
    self.skipWaiting()
    console.log('swm: installed, skipped waiting')
})