(ns clojure-getting-started.web-test
  (:use midje.sweet)
  (:require [clojure-getting-started.web :as web]))

(facts
 (+ 1 2 3) => 6)
