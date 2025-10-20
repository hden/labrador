(ns labrador.impl.walker
  (:refer-clojure :exclude [into])
  (:require [urania.core :as u]
            [labrador.impl.core :refer [value value?]]))

(defn- into
  ([to from] (into to (map identity) from))
  ([to xform from]
   (->> from
        (clojure.core/into [] (comp xform (map value)))
        (u/collect)
        (u/map #(clojure.core/into to %)))))

(defmulti coerce
  "Coerce arbitrary Clojure data `v` into a muse of its 'coerced' form.
   Reinterpretation (lossy) by design. Dispatch by shape; default = (value v)."
  (fn [v]
    (cond
      (value? v)        ::muse
      (record? v)       ::map
      (map? v)          ::map
      (list? v)         ::collection
      (vector? v)       ::collection
      (set? v)          ::collection
      (sequential? v)   ::collection
      :else             (class v))))

(defmethod coerce ::muse [v] v)

(defmethod coerce ::map [m]
  (into (empty m)
        (map (fn [[k v]]
               (u/map vector (coerce k) (coerce v))))
        m))

(defmethod coerce ::collection [xs]
  (->> (into [] (map coerce) xs)
       (u/map (fn [ys]
                (if (seq? xs)
                  (apply list ys)
                  (clojure.core/into (empty xs) ys))))))

(defmethod coerce :default [x]
  (value x))
