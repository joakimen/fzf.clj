(ns fzf.core-test
  (:require [clojure.test :as t]
            [fzf.core :as fzf])
  (:import (java.lang AssertionError)))

(t/deftest fail-with-exception
  (t/testing "Using both :preview and :preview-fn causes exception"
    (t/is (thrown? AssertionError (fzf/fzf {:preview "foo" :preview-fn (fn [_] "bar")} ["a" "b" "c"])))))
