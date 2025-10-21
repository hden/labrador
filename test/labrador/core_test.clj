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
   :decorate-fn
   (fn [value & params]
     (assoc value :params params :friend (core/fetch :golden 1)))})
 
(deftest good-boi
  (testing "basic retriever"
    (let [env {:a :b}
          root (core/fetch :golden 1 :params)]
      (is (value? root))
      (is (= {:env env
              :ids #{1}}
             (u/run!! root {:env env})))
      (is (= [{:foo {:env env
                     :ids #{1}}}]
             (u/run!! (core/traverse [{:foo root}])
                      {:env env})))))
  
  (testing "relational retriever"
    (let [env {:a :b}
          root (core/fetch :labrador 2 :foo :bar :baz)]
      (is (= {:env    env
              :ids    #{2}
              :params [:foo :bar :baz]
              :friend {:env {:a :b}
                       :ids #{1}}}
             (u/run!! (core/traverse root) {:env env}))))))

(core/defretriever a
  {:backend-fn
   (fn [& _]
     (throw (ex-info "oops" {})))})

(core/defretriever b
  {:backend-fn backend
   :decorate-fn
   (fn [& _]
     (throw (ex-info "oops" {})))})

(core/defretriever c
  {:backend-fn (constantly nil)})

(core/defretriever d
  {:backend-fn backend
   :decorate-fn (constantly nil)})

(deftest naughty-tests
  (testing "throwing in backend-fn"
    (let [env {:a :b}
          root (core/fetch :a 1)
          ex (atom nil)]
      (try
        (u/run!! (core/traverse root) {:env env})
        (catch Exception e
          (reset! ex e)))
      (is (instance? Exception @ex))))
  
  (testing "throwing in decorate-fn"
    (let [env {:a :b}
          root (core/fetch :b 1)
          ex (atom nil)]
      (try
        (u/run!! (core/traverse root) {:env env})
        (catch Exception e
          (reset! ex e)))
      (is (instance? Exception @ex)))))

(deftest nil-punning-tests
  (testing "returning nil in backend-fn"
    (let [env {:a :b}
          root (core/fetch :c 1)
          res (u/run!! (core/traverse root) {:env env})]
      (is (nil? res)))) 

  (testing "returning nil in decorate-fn"
    (let [env {:a :b}
          root (core/fetch :d 1)
          res (u/run!! (core/traverse root) {:env env})]
      (is (nil? res)))))
