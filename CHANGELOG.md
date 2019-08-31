# Change Log


## [0.4.0] - 2019-08-01
### Fixed
- `stylesheet-async` parameter working with a collection of stylesheet paths.
### Improved
Readme - new section on service workers and PWA assets params.
### Changed
License is now MIT.

## [0.4.0-alpha] - 2019-08-01

### New
Experimental service worker generation. Pass the same map you use
to generate a page and this function will generate a service worker
that will pre-cache all the assets found in the map.
Simple cache-busting included.
`page-renderer.api/generate-service-worker`
Two additional options:
- `renderable.sw-default-url` default route for your service worker, usually
  the same as `start_url` in your `manifest.json` if you do a PWA.
- `renderable.sw-add-assets` a vector with additional assets to pre-cache

`page-renderer.api` namespace
- Same public members as in `page-renderer.core` plus `generate-service-worker` 
  and `respond-service-worker`

### Added to `page-renderer.api/render-page`
- Service worker lifecycle script, just add `service-worker` key
- Noscript fallback for async stylesheet appender script
- Support for favicon extensions other than .png
- Support for apple-touch-icon, apple-touch-startup-image, image_src meta



## [0.3.1] - 2019-07-06

### Added to `page-renderer.core/render-page`
- on-dom-interactive-js JS snippet. If you need to do something in JS
  when DOM is interactive (that's before `load` event).
- `stylesheet-async` option: now supports a collection
- `js-module` option: now supports a collection
- Language support: provide `lang` param to render lang attr on the
  `<html>` tag and a HTTP-Equiv meta tag.
- PWA Manifest support

### Changed
- `page-renderer.core/render-page`
  Now provides default /favicon.png and cache-busts it



## [0.2.2] - 2019-06-22
### Changed
- `page-renderer.core/render-page`
  Render twitter meta if either `:twitter-site` or `:twitter-creator` are supplied.
  Previously worked only if `:twitter-site` was supplied.
