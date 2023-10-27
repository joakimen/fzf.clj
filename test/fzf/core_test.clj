(ns fzf.core-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :as t]
            [fzf.core :as fzf])
  (:import (java.lang AssertionError)))

(t/deftest fail-with-exception
  (t/testing "Using both :preview and :preview-fn causes exception"
    (t/is (thrown? AssertionError (fzf/fzf {:preview "foo" :preview-fn (fn [_] "bar")} ["a" "b" "c"])))))

(t/deftest spec-test

  (t/testing "valid args should pass"
    (t/is (s/valid? :fzf/args ["a" "b" "c"]))
    (t/is (s/valid? :fzf/args [1 2 3]))
    (t/is (s/valid? :fzf/args [:one :two :three]))
    (t/is (s/valid? :fzf/args [1.234 4.0 -82.18888888]))
    (t/is (s/valid? :fzf/args (keys {:a 1 :b 2}))))

  (t/testing "invalid args should fail"
    (t/is (not (s/valid? :fzf/args "a")))
    (t/is (not (s/valid? :fzf/args 1)))
    (t/is (not (s/valid? :fzf/args :one)))
    (t/is (not (s/valid? :fzf/args 1.23)))
    (t/is (not (s/valid? :fzf/args {:a 1})))))
