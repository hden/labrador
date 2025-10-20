(defproject com.github.hden/labrador "0.1.0-SNAPSHOT"
  :description "A dataloader library based on Urania"
  :url "https://github.com/hden/labrador"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.3"]
                 [funcool/promesa "11.0.678"]
                 [funcool/urania "0.2.0"]]
  :repl-options {:init-ns labrador.core}
  :plugins [[lein-cloverage "1.2.4"]])
