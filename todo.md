
# Done

= 06 Jul 2019 =
Manifest support
Add Language support with lang on doc and a meta tag
  [:meta {:http-equiv "Content-Language" :content "en"}]
Support multiple async stylesheets
Support JS init on dom interactive
- service worker generation
- fix cachebusting hashing, document it

= 22 Mar 2020 =
- 0.4.5 fix service worker default url (allow to accept "/" as default)
- 0.4.6 improve docs for page-renderer.api namespace
- update readme and changelog


# In progress 


# Planned

## Garden
  - invoke garden compiler via requiring-resolve, so that it could be used optionally.
  - drop Garden dependency

# Doc-attrs
  - doc-attrs : allow edn inline
  - doc-attrs : fix doc-attrs treatment for longs and booleans
  - doc : add doc-attrs

# API2
- use fully qualified keywords

- ns to parse pages into page frames. use in tests to check for links
  https://github.com/davidsantiago/hickory
- use typography  https://gist.github.com/spacegangster/10b5b0a9bba9e028d713c2d0748c8abd


- use :service-worker key in blacklist-regex
- support multiple favicons


- cache-bust : check if cache-busting nullifies path keys
- cache-bust : add cache-bust-resource memeber

- rework stylesheet inlining
- investigate manifest can avoid cachebusting 
- hooks for preloading / preconnecting / prefetching



https://news.ycombinator.com/item?id=23368147#23380770

I'm relatively new to Clojure and have been interested in writing something like this. I have a few questions/comments:
- What made you decide to write CacheBustHelper in Java? I believe something like:

  (defn bust-cache
    [input-stream]
    (with-open [rdr (clojure.java.io/reader input-stream)]
      (doseq [b (.read rdr)
              :when (not= -1 b)]
        nil)))
I'm not sure the performance difference between the two though.
- Does it work with any web backend framework? Could I use something like Luminus or Fulcro? Does the request just have to follow the Ring request format?

- Looks like lines 183 through 198 on core.clj could be when instead of if

- what about making update-if-present:

  (defn update-if-present
    [m & v]
      (reduce (fn [acc [k f]] 
                (if (contains? acc k) 
                  (update acc k f)
                  acc))
              m
              (partition 2 v)))
That would allow you to do the following:
  (update-if-present page-data :twitter-image cache-bust-one
                               :og-image cache-bust-one
                               :favicon cache-bust-one
                               :link-apple-icon cache-bust-one
                               :link-apple-startup-image cache-bust-one
                               :link-image-src cache-bust-one
                               :script cache-bust
                               :script-sync cache-bust
                               :js-module cache-bust
                               :stylesheet cache-bust
                               :stylesheet-async cache-bust
                               :manifest cache-bust-one)
Those are just some things I noticed on first glance.
