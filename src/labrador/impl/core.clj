(ns labrador.impl.core
  (:require [urania.core :as u]))

(defn value?
  "Return true if x is a Urania async value (muse)."
  [x]
  (or (satisfies? u/AST x)
      (satisfies? u/DataSource x)))

(defn value
  "Wrap x in a muse; pass-through if already a muse."
  [x] (if (value? x) x (u/value x)))
