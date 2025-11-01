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
  [env ids]
  (backend env ids))

(core/defretriever labrador
  {:decorate-fn
   (fn [value & params]
     (assoc value :params params :friend (core/fetch ::golden 1)))}
  [env ids]
  (backend env ids))

(core/defretriever custom
  {:tag :dog/custom}
  [env ids]
  (backend env ids))

(deftest good-boi
  (testing "basic retriever"
    (let [env {:a :b}
          root (core/fetch ::golden 1 :params)]
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
          root (core/fetch ::labrador 2 :foo :bar :baz)]
      (is (= {:env    env
              :ids    #{2}
              :params [:foo :bar :baz]
              :friend {:env {:a :b}
                       :ids #{1}}}
             (u/run!! (core/traverse root) {:env env}))))))

(core/defretriever a
  [_ _]
  (throw (ex-info "oops" {})))

(core/defretriever b
  {:decorate-fn
   (fn [& _]
     (throw (ex-info "oops" {})))}
  [env ids]
  (backend env ids))

(core/defretriever c
  [_ _]
  nil)

(core/defretriever d
  {:decorate-fn (constantly nil)}
  [env ids]
  (backend env ids))

(deftest naughty-tests
  (testing "throwing in backend-fn"
    (let [env {:a :b}
          root (core/fetch ::a 1)
          ex (atom nil)]
      (try
        (u/run!! (core/traverse root) {:env env})
        (catch Exception e
          (reset! ex e)))
      (is (instance? Exception @ex))))
  
  (testing "throwing in decorate-fn"
    (let [env {:a :b}
          root (core/fetch ::b 1)
          ex (atom nil)]
      (try
        (u/run!! (core/traverse root) {:env env})
        (catch Exception e
          (reset! ex e)))
      (is (instance? Exception @ex)))))

(deftest nil-punning-tests
  (testing "returning nil in backend-fn"
    (let [env {:a :b}
          root (core/fetch ::c 1)
          res (u/run!! (core/traverse root) {:env env})]
      (is (nil? res)))) 

  (testing "returning nil in decorate-fn"
    (let [env {:a :b}
          root (core/fetch ::d 1)
          res (u/run!! (core/traverse root) {:env env})]
      (is (nil? res)))))

(deftest custom-tag-tests
  (testing "custom tag dispatch works"
    (let [env {:a :b}
          root (core/fetch :dog/custom 9)]
      (is (= {:env env
              :ids #{9}}
             (u/run!! (core/traverse root) {:env env})))))
  (testing "custom tag registration replaces default"
    (is (contains? (methods core/fetch) :dog/custom))
    (is (not (contains? (methods core/fetch) ::custom)))))

(deftest metadata-tests
  (testing "default tag is namespace-qualified"
    (is (contains? (methods core/fetch) ::golden))))
