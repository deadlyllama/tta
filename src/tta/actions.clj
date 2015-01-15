(ns tta.actions
  (:use tta.papu))

(defn run-action [action game]
  (let [requirement-results (map #(% game)
                                 (:requirements action))
        failing (filter (complement :ok?) requirement-results)
        failing-messages (set (mapcat :messages failing))]
    (if (empty? failing)
      (let [result ((:action action) game)]
        {:result (:result result)
         :ok? true
         :messages (:messages result)})
      {:result game
       :ok? false
       :messages failing-messages})))

(def increase-population-action
  (combine pay-population-increase-cost
           decrease-population-pool
           increase-worker-pool
           pay-action))

(def build-farm-action
  (build-building :building-action increase-farms
                  :resource-cost 2
                  :building-name "farm"))

(def build-mine-action
  (build-building :building-action increase-mines
                  :resource-cost 2
                  :building-name "mine"))

(def build-temple-action
  (build-building :building-action increase-temples
                  :resource-cost 3
                  :building-name "temple"))

(def build-lab-action
  (build-building :building-action increase-labs
                  :resource-cost 3
                  :building-name "lab"))
