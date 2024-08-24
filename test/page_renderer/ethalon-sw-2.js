importScripts('https://storage.googleapis.com/workbox-cdn/releases/6.5.4/workbox-sw.js')

console.log('Service Worker: ', self);

workbox.precaching.precacheAndRoute([
    { url: '/', revision: 'file-hash' },
    { url: '/fonts/icomoon.woff', revision: 'file-hash' },
    { url: '/lightpad/compiled/app.js', revision: 'file-hash' },
    { url: '/favicon.png', revision: 'file-hash' }
], { ignoreURLParametersMatching: [/hash/] });


// default handler
const defaultHandler = new workbox.strategies.CacheFirst({
    cacheName: 'default-handler-cache',
});


// routing
workbox.routing.registerRoute(
    new workbox.routing.NavigationRoute(
        defaultHandler,
        workbox.precaching.getCacheKeyForURL('/'), {
            whitelist: [ /^\// ],
            blacklist: [ /^\/service-worker\.js/ ]
        }
    )
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