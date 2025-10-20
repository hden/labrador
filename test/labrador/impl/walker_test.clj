(ns labrador.impl.walker-test
  (:require [clojure.test :refer [deftest is]]
            [urania.core :as u]
            [labrador.impl.core :refer [value value?]]
            [labrador.impl.walker :as walker]))
 
(deftest coerce-mixed-structure
  (let [inst #inst"2025-01-01"
        data {(value "key") [:a (value :b) :c]
              :v            (vector (value 1) (value 2))
              :s            #{(value 1) (value 2)}
              :l            (list :x :y :z)
              :x            inst}
        muse (walker/coerce data)
        res  (u/run!! muse {})]
    ;; keys resolved, values resolved, order rules respected
    (is (value? muse))
    (is (= {"key" [:a :b :c]
            :v    [1 2]
            :s    #{1 2}
            :l    `(:x :y :z)
            :x    inst}
           res))))
