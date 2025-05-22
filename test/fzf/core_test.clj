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
    (t/is (not (s/valid? :fzf/args {:a 1}))))

  (t/testing "valid opts for bindings should pass"
    (t/is (s/valid? :fzf/opts {:additional-bindings ["ctrl-a:accept"]}))
    (t/is (s/valid? :fzf/opts {:additional-bindings []}))
    (t/is (s/valid? :fzf/opts {:command-bindings {"ctrl-x" ["accept"]}}))
    (t/is (s/valid? :fzf/opts {:command-bindings {"ctrl-x" [{:action-name "reload" :command-string "ls -l"}]}}))
    (t/is (s/valid? :fzf/opts {:command-bindings {"ctrl-x" [{:action-name "execute" :handler-fn 'user/my-fn}]}}))
    (t/is (s/valid? :fzf/opts {:command-bindings {"ctrl-x" [{:action-name "change-prompt" :simple-arg "New>"}]}}))
    (t/is (s/valid? :fzf/opts {:command-bindings {"ctrl-x" [{:action-name "reload" :bbnc-reload-fn (fn [_] ["item"])}]}}))
    (t/is (s/valid? :fzf/opts {:command-bindings {"ctrl-x" ["accept" {:action-name "reload" :command-string "ls"}]}}))
    (t/is (s/valid? :fzf/opts {:command-bindings {}})))

  (t/testing "invalid opts for bindings should fail"
    (t/is (not (s/valid? :fzf/opts {:additional-bindings "ctrl-a:accept"}))) ; Not a collection
    (t/is (not (s/valid? :fzf/opts {:additional-bindings [123]})))           ; Collection of non-strings
    (t/is (not (s/valid? :fzf/opts {:command-bindings {"ctrl-x" "accept"}}))) ; Value not a vector
    (t/is (not (s/valid? :fzf/opts {:command-bindings {"ctrl-x" [{:command-string "ls"}]}}))) ; Missing :action-name
    (t/is (not (s/valid? :fzf/opts {:command-bindings {"ctrl-x" [{:action-name "reload"
                                                                  :command-string "ls"
                                                                  :simple-arg "foo"}]}}))) ; Multiple arg types
    (t/is (not (s/valid? :fzf/opts {:command-bindings {"ctrl-x" [{:action-name "execute"
                                                                  :bbnc-reload-fn (fn [_] ["item"])}]}}))) ; :bbnc-reload-fn with non-reload action
    (t/is (not (s/valid? :fzf/opts {:command-bindings {"ctrl-x" [123]}}))) ; Action spec not string or map
    (t/is (not (s/valid? :fzf/opts {:command-bindings {"ctrl-x" [{:action-name "reload" :bbnc-reload-fn "not-a-fn"}]}}))))) ; :bbnc-reload-fn not a fn

(t/deftest fail-with-exception
  (t/testing "Using both :preview and :preview-fn causes exception"
    (t/is (thrown? AssertionError (fzf/fzf {:preview "foo" :preview-fn (fn [_] "bar")} ["a" "b" "c"]))))

  (t/testing "Invalid :additional-bindings causes exception"
    (t/is (thrown? AssertionError (fzf/fzf {:additional-bindings "not-a-collection"} ["a"])))
    (t/is (thrown? AssertionError (fzf/fzf {:additional-bindings [123]} ["a"]))))

  (t/testing "Invalid :command-bindings causes exception"
    (t/is (thrown? AssertionError (fzf/fzf {:command-bindings {"key" "not-a-vector"}} ["a"])))
    (t/is (thrown? AssertionError (fzf/fzf {:command-bindings {"key" [{:no-action-name "foo"}]}} ["a"])))
    (t/is (thrown? AssertionError (fzf/fzf {:command-bindings {"key" [{:action-name "reload"
                                                                       :command-string "ls"
                                                                       :simple-arg "foo"}]}} ["a"])))
    (t/is (thrown? AssertionError (fzf/fzf {:command-bindings {"key" [{:action-name "execute"
                                                                       :bbnc-reload-fn (fn [_] [])}]}} ["a"])))
    (t/is (not (s/valid? :fzf/args {:a 1})))))
