(ns labrador.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [promesa.core :as p]
            [urania.core :as u]
            [labrador.impl.core :refer [value?]]
            [labrador.core :as core]))

(defn backend [env ids]
  (let [res (into {}
                  (map (fn [id]
                         [id {:env env :ids ids}]))
                  ids)]
    (p/resolved res)))

(core/defretriever golden
  {:backend-fn backend})

(core/defretriever labrador
  {:backend-fn backend
   :decorate-fn (fn [value & params]
                  (assoc value :params params :friend (core/fetch :golden 1)))})
 
(deftest main-test
  (testing "basic retriever"
    (let [env {:a :b}
          node (core/fetch :golden 1 :params)]
      (is (value? node))
      (is (= {:env env
              :ids #{1}}
             (u/run!! node {:env env})))
      (is (= [{:foo {:env env
                     :ids #{1}}}]
             (u/run!! (core/traverse [{:foo node}])
                      {:env env})))))
  
  (testing "relational retriever"
    (let [env {:a :b}
          node (core/fetch :labrador 2 :foo :bar :baz)]
      (is (= {:env    env
              :ids    #{2}
              :params [:foo :bar :baz]
              :friend {:env {:a :b}
                       :ids #{1}}}
             (u/run!! (core/traverse node) {:env env}))))))
