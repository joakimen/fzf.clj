(ns fzf.impl-test
  (:require [fzf.impl :as i]
            [clojure.test :as t]))

(t/deftest internal
  (t/is (= "placeholder" (i/fzf))))
