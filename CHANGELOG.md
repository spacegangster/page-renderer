# Change Log


## [0.3.1] - 2019-06-22

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
