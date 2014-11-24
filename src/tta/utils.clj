(ns tta.utils
  (:use [clojure.algo.monads :only [defmonad]]))

(defn singleton? [coll]
  (and (not (empty? coll))
       (empty? (rest coll))))

(defn multi-assoc-in [target & path-value-pairs]
  (if (or (empty? path-value-pairs)
          (singleton? path-value-pairs))
    target
    (let [[path value] (take 2 path-value-pairs)
          rest-pairs (drop 2 path-value-pairs)]
      (apply multi-assoc-in (assoc-in target path value)
                            rest-pairs))))

(defn no-messages [a-value]
  {:result a-value
   :messages []})

(defmonad message-m
  [m-result no-messages
   m-bind   (fn [a-value f]
              (let [result (f (:result a-value))]
                {:result (:result result)
                 :messages (concat (:messages a-value)
                                   (:messages result))}))])

(defn write [message]
  (fn [a-value]
    {:result a-value
     :messages [message]}))

(defn messageless [f]
  (fn [a-value]
    {:result (f a-value)
     :messages []}))
