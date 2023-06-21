(ns fzf.spec
  (:require [babashka.fs :as fs]
            [clojure.spec.alpha :as s]))

(s/def :fzf/header-str string?)
(s/def :fzf/header-lines int?)
(s/def :fzf/header-first boolean?)
(s/def :fzf/header
  (s/keys :opt [:fzf/header-str
                :fzf/header-lines
                :fzf/header-first]))

(s/def :fzf/in (s/coll-of string?))
(s/def :fzf/dir fs/directory?)
(s/def :fzf/multi boolean?)
(s/def :fzf/preview string?)

(s/def :fzf/reverse boolean?)
(s/def :fzf/height (s/and string? #(re-matches #"^~?\d+%?$" %)))
(s/def :fzf/tac boolean?)
(s/def :fzf/case-insensitive boolean?)
(s/def :fzf/exact boolean?)

(s/def :fzf/opts
  (s/keys
   :opt-un [:fzf/in
            :fzf/dir
            :fzf/multi
            :fzf/preview
            :fzf/reverse
            :fzf/header
            :fzf/height
            :fzf/tac
            :fzf/case-insensitive
            :fzf/exact]))
