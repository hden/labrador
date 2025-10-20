(ns labrador.impl.retriever-test
  (:require [clojure.test :refer [deftest testing is]]
            [promesa.core :as p]
            [urania.core :as u]
            [labrador.impl.core :refer [value]]
            [labrador.impl.retriever :as r]))
 
(deftest fetch-test
  (testing "promise-returning backend"
    (let [query #{:a :b :c}
          f (constantly (p/resolved {:a 1 :b 2}))
          res (p/await (r/fetch {:backend-fn f} query :env))]
      (is (= {:a 1 :b 2 :c nil}
             res))))

  (testing "value-returning backend"
   (let [query #{:a :b :c}
         f (constantly {:a 1 :b 2})
         res (p/await (r/fetch {:backend-fn f} query :env))]
     (is (= {:a 1 :b 2 :c nil}
            res)))))


(deftest decorate-test
  (let [f (fn [a b] (merge a b))
        res (u/run!! (r/decorate {:decorate-fn f} (value {:a 1}) {:b 2}))]
    (is (= {:a 1 :b 2}
           res))))
