(ns build
  (:refer-clojure :exclude [test])
  (:require [org.corfield.build :as bb]))

(def lib 'net.clojars.io-orkes/clojure-sdk-examples)
(def version "0.1.0-SNAPSHOT")
(def main 'io-orkes.clojure-sdk-examples)

(defn test "Run the tests." [opts]
  (bb/run-tests opts))

(defn ci "Run the CI pipeline of tests (and build the uberjar)." [opts]
  (-> opts
      (assoc :lib lib :version version :main main)
      (bb/run-tests)
      (bb/clean)
      (bb/uber)))
