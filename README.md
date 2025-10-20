# Labrador

A [dataloader](https://github.com/graphql/dataloader) library based on [Urania](https://github.com/funcool/urania). Batch data fetching across arbitrary Clojure data structures and resolve everything with a single `run!!`. Duplicate requests for the same id are automatically coalesced and fetched in batches.

## Why Labrador?

* Erase N+1: Scatter fetches through your data; resolve once at the end.
* Automatic coalescing: Multiple requests for the same id are merged into a single backend call.
* Just data: Write plain Clojure data. Asynchrony is handled by `traverse`.
* Composable finishing step: Use `decorate-fn` to shape results and even attach additional fetches—`traverse` will resolve those too.

## Quick start

```clj
(ns example
  (:require [promesa.core :as p]
            [urania.core :as u]
            [labrador.core :as lab]))

;; 1) Backend contract
(defn backend
  "Accepts (env ids) and returns a promise or a plain map {id -> value}."
  [env ids]
  (p/presolved
    (into {} (map (fn [id] [id {:env env :id id}])) ids)))

;; 2) Define an entry point: (lab/fetch :user id & more)
(lab/defretriever user
  {:tag         :user                      ; optional (default is ns-qualified)
   :backend-fn  backend                    ; required
   :decorate-fn (fn [x & more]             ; optional
                  ;; You may embed further fetches; traverse resolves them too.
                  (assoc x :more more :friend (lab/fetch :user 1)))})

;; 3) Fetch one value (returns an async value)
(def user-42 (lab/fetch :user 42 :foo :bar))

;; 4) Resolve once (all coalesced and batched)
(u/run!! user-42 {:env {:locale "ja"}})
;; => {:env {:locale "ja"}, :id 42, :more (:foo :bar)}

;; 5) Traverse arbitrary structures (keys are traversed too)
(u/run!! (lab/traverse
          {:k (lab/fetch :user 2)
           (lab/fetch :user 3) [:a (lab/fetch :user 4)]})
        {:env {:locale "ja"}})
;; => {:k {:env ... :id 2}, {:env ... :id 3} [:a {:env ... :id 4}]}
```

## How it works (without internal jargon)

* **Single resolve**: You can place async values anywhere in your data (even in map keys). lab/traverse walks the structure and resolves everything in one go.
* **Batching & coalescing**: Requests for the same id are merged; your backend receives a set/collection of unique ids per batch.
* **Decorate after fetch**: decorate-fn shapes the just-fetched value. If it returns a structure containing more fetches, those are picked up by traverse and resolved in the same pass when possible.
* **Lossy but pragmatic**: The library reinterprets structures for ease of use—e.g., records default to plain maps; lists keep order.

## API

### `lab/defretriever`

```clj
(lab/defretriever name
  {:backend-fn  (fn [env ids] -> promise-or-map {id -> value})   ; required
   :decorate-fn (fn [value & more] -> value-or-structure)        ; optional
   :tag         :user})                                          ; optional
```

What it generates:

* A record `name` representing a batchable fetch unit.
* A `lab/fetch` multimethod entry so you can call `(lab/fetch :tag id & more)`.

#### Backend contract

* Input: env (your execution context), ids (all unique ids to fetch in this batch).
* Output: a map {id -> value}, either returned immediately or wrapped in a promise. Missing ids are filled with nil by the library.

#### Decorate contract

* `(fn [value & more] ...)`.
* Return any Clojure structure; it may include more `lab/fetch` calls—`lab/traverse` will spot and resolve them.

### `lab/fetch`

```clj
(lab/fetch :user 42 :foo :bar) ; -> async value
```

* The extra arguments `:foo :bar` are passed to your `decorate-fn` as-is (flat).

### `lab/traverse`

```clj
(lab/traverse data-structure-with-fetches) ; -> async value of the whole structure
```

* Walks maps (both keys and values), vectors, lists, sets, and general seqs.
* Lists preserve order. Records default to plain maps.

## Glossary

* **Async value**: What lab/fetch and lab/traverse return; resolve with urania.core/run!!.
* **Retriever**: A fetch unit defined via lab/defretriever. It batches and coalesces duplicate requests behind the scenes.
* **Env (environment)**: Runtime context passed to the backend—DB handles, locale, request metadata, etc.
* **Decorate**: A light, pure “finishing step” applied after fetching; a natural place to assemble related fetches.

## Design goals

* **Resolve once**: You control structure; the library handles asynchrony.
* **Duplicate-friendly**: Asking for the same id multiple times is fine; it’s merged under the hood.
* **Composable**: Decorating a value with more fetches composes naturally—no manual orchestration.

## Inspiration / Acknowledgements

Labrador draws inspiration from outstanding prior art:

* [superlifter](https://github.com/oliyh/superlifter) — by oliyh

## License

Copyright © 2025 Haokang Den

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
https://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
