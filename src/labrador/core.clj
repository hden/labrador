(ns labrador.core
  (:require [labrador.impl.walker :as walker]
            [labrador.impl.retriever :as retriever]))

(def traverse walker/coerce)

(defmulti fetch (fn [collection & _] collection))

(defmacro defretriever
  "Define a Urania DataSource + BatchedSource pair with a uniform backend.

   Signature mirrors `defn`:

   (defretriever name
     \"optional docstring\"
     {:decorate-fn (fn [value & more] ...)
      :tag         :optional-tag}
     [env ids & more]
     body)

   The function body becomes the backend implementation and must return
   either a promise or a map of {id -> value}. The generated retriever
   is then available via `(fetch :tag id & decorate-args)` (or use
   `(fetch ::name ...)` when you skip `:tag`)."
  {:arglists '([name doc-string? attr-map? [params*] prepost-map? body]
               [name doc-string? attr-map? ([params*] prepost-map? body) + attr-map?])
   :clj-kondo/ignore  [:unresolved-symbol]}
  [sym & fdecl]
  (let [docstring   (when (string? (first fdecl)) (first fdecl))
        fdecl*      (if docstring (next fdecl) fdecl)
        first-map   (when (map? (first fdecl*)) (first fdecl*))
        attr-map?   (when first-map
                      (not-any? #(contains? first-map %)
                                [:decorate-fn :tag]))
        attr-map    (when attr-map? first-map)
        fdecl**     (if attr-map (next fdecl*) fdecl*)
        options-map? (map? (first fdecl**))
        opts-expr   (if options-map? (first fdecl**) {})
        fn-tail     (if options-map? (next fdecl**) fdecl**)
        _           (when-not (seq fn-tail)
                      (throw (IllegalArgumentException.
                              "defretriever requires a function body")))
        env*        (gensym "env__")
        ents*       (gensym "entities__")
        args*       (gensym "args__")
        options*    (gensym "options__")
        ctx-map*    (gensym "ctx__")
        tag*        (gensym "tag__")
        _sym        (symbol "_")
        id-field    (symbol "id")
        ctx-field   (symbol "context")
        record-sym  (symbol (str (name sym) "-retriever"))
        ctor-sym    (symbol (str "->" (name record-sym)))
        fn-sym      (let [base (symbol (name sym))]
                      (if-let [m (meta sym)]
                        (with-meta base m)
                        base))]
    `(do
       (defn ~fn-sym
         ~@(cond-> []
             docstring (conj docstring)
             attr-map  (conj attr-map))
         ~@fn-tail)

       (defrecord ~record-sym [~id-field ~ctx-field]
         urania.core/DataSource
         (-identity [~_sym] ~id-field)
         (-fetch [~_sym ~env*]
           (promesa.core/then
            (labrador.impl.retriever/fetch ~ctx-field #{~id-field} ~env*)
            (fn [m#] (get m# ~id-field))))

         urania.core/BatchedSource
         (-fetch-multi [~_sym ~ents* ~env*]
           (let [uniq-ids# (into #{~id-field} (map :id) ~ents*)]
             (labrador.impl.retriever/fetch ~ctx-field uniq-ids# ~env*))))

       (let [~options* ~opts-expr
             ~tag*     (get ~options* :tag ~(keyword (str (ns-name *ns*)) (name sym)))
             ~ctx-map* (assoc ~options* :backend-fn ~fn-sym :tag ~tag*)]
         (defmethod labrador.core/fetch ~tag*
           [~_sym ~id-field & ~args*]
           (let [node# (~ctor-sym ~id-field ~ctx-map*)]
             (labrador.impl.retriever/decorate ~ctx-map* node# ~args*)))))))
