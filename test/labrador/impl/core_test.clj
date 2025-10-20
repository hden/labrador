(ns labrador.impl.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [urania.core :as u] 
            [labrador.impl.core :as core]))

(deftest core-test
  (testing "value?"
    (is (core/value? (u/value :a)))
    (is (not (core/value? :a))))
  
  (testing "value"
    (is (core/value? (core/value :a)))
    (let [a (u/value :a)]
      (is (= a (core/value a))))))
