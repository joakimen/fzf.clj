(ns fzf.impl-test
  (:require [fzf.impl :as i]
            [clojure.test :as t]))

(t/deftest parse-opts-test
  (t/testing "no options produce expected defaults"
    (t/is (= {:cmd ["fzf"], :opts {:in :inherit, :out :string, :err :inherit}}
             (i/parse-opts {} []))))
  (t/testing "adding fzf flags produce correct command string"
    (t/is (= ["fzf" "--multi" "--reverse" "--tac" "-i" "--exact" "--preview" "echo {}" "--header" "header-text" "--header-lines" 2 "--header-first" "--height" "10%"]
             (:cmd (i/parse-opts {:multi true
                                  :reverse true
                                  :tac true
                                  :case-insensitive true
                                  :exact true
                                  :preview "echo {}"
                                  :header {:header-str "header-text"
                                           :header-lines 2
                                           :header-first true}
                                  :height "10%"}
                                 [])))))
  (t/testing "providing input-arguments maps them to process stdin"
    (t/is (= {:in "one\ntwo\nthree", :out :string, :err :inherit}
             (:opts (i/parse-opts {} ["one" "two" "three"]))))
    (t/is (= {:in "1\n2\n3", :out :string, :err :inherit}
             (:opts (i/parse-opts {} [1 2 3]))))
    (t/is (= {:in ":one\n:two\n:three", :out :string, :err :inherit}
             (:opts (i/parse-opts {} [:one :two :three])))))
  (t/testing "providing no input-arguments should retain default proc options"
    (t/is (= {:in :inherit, :out :string, :err :inherit}
             (:opts (i/parse-opts {} [])))))
  (t/testing "option for startup-dir to correctly mapped to process options"
    (t/is (= {:in :inherit, :out :string, :err :inherit, :dir "/tmp"}
             (:opts (i/parse-opts {:dir "/tmp"} []))))))
