(ns labrador.core
  (:require [labrador.impl.walker :as walker]
            [labrador.impl.retriever :as retriever]))

(def traverse walker/coerce)

(defmulti fetch (fn [collection & _] collection))

(defmacro defretriever
  "Define a Urania DataSource + BatchedSource pair with a uniform backend.
   opts:
   - :backend-fn  (required) (fn [env ids] -> promise of {id -> value})
   - :decorate-fn (optional) (fn [value params & more] -> value)
   - :tag         (optional) keyword for (fetch :tag ...)"
  {:clj-kondo/ignore [:unresolved-symbol]}
  [sym opts]
  (let [env*      (gensym "env__")
        ents*     (gensym "entities__")
        args*     (gensym "args__")
        tag*      (gensym "tag__")
        _         (symbol "_")
        id-field  (symbol "id")
        ctx-field (symbol "context")]
    `(do
       (defrecord ~sym [~id-field ~ctx-field]
         urania.core/DataSource
         (-identity [~_] ~id-field)
         (-fetch    [~_ ~env*]
           (promesa.core/then
            (labrador.impl.retriever/fetch ~ctx-field #{~id-field} ~env*)
            (fn [m#] (get m# ~id-field))))
         
         urania.core/BatchedSource
         (-fetch-multi [~_ ~ents* ~env*]
           (let [uniq-ids# (into #{~id-field} (map :id) ~ents*)]
             (labrador.impl.retriever/fetch ~ctx-field uniq-ids# ~env*))))
       
       (let [~tag* (get ~opts :tag ~(keyword (name sym)))]
         (defmethod labrador.core/fetch ~tag* [~_ ~id-field & ~args*]
           (let [node# (new ~sym ~id-field ~opts)]
             (labrador.impl.retriever/decorate ~opts node# ~args*)))))))
