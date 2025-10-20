(ns labrador.impl.retriever
  (:require [promesa.core :as p]
            [urania.core :as u]
            [labrador.impl.core :refer [value]]
            [labrador.impl.walker :refer [coerce]]))

(defn fetch [{:keys [backend-fn]} ids env]
  (p/then (p/promise (backend-fn env ids))
          (fn [res]
            (let [m (zipmap ids (repeat nil))]
              (merge m res)))))

(defn decorate
  [{:keys [decorate-fn]} node params]
  (if decorate-fn
    (u/mapcat (fn [x]  
                (if (nil? x) 
                  (value nil)
                  (coerce (apply decorate-fn x params))))
              node)
    node))
