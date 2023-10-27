(ns fzf.core
  "Public interface to the fzf-wrapper"
  (:require [babashka.fs :as fs]
            [clojure.spec.alpha :as s]
            [fzf.impl :as i]))

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
(s/def :fzf/preview-fn fn?)

(s/def :fzf/reverse boolean?)
(s/def :fzf/height (s/and string? #(re-matches #"^~?\d+%?$" %)))
(s/def :fzf/tac boolean?)
(s/def :fzf/case-insensitive boolean?)
(s/def :fzf/exact boolean?)

(s/def :fzf/opts
  (s/and  (s/keys
           :opt-un [:fzf/in
                    :fzf/dir
                    :fzf/multi
                    :fzf/preview
                    :fzf/preview-fn
                    :fzf/reverse
                    :fzf/header
                    :fzf/height
                    :fzf/tac
                    :fzf/case-insensitive
                    :fzf/exact])
          #(not (and (:preview %) (:preview-fn %)))))

(s/def :fzf/args vector?)

(defn fzf
  "Public interface to fzf.

   `opts`: Options map (all keys are optional)
   - dir: String indicating the startup-dir of the fzf-command
   - multi: Bool, toggles multi-select in fzf. If true, fzf returns a vector instead of string
   - preview: String, preview-command for the currently selected item
   - preview-fn: Function, preview function that will be called on the currently selected item.
                 Its return value will be displayed in the preview window.
                 :preview-fn cannot be used in combination with :preview, i.e.
                 only one of them can be used for a single invocation of fzf.
   - reverse: Bool, reverse the order of the fzf input dialogue
   - header: Map with sticky-header options for the fzf input dialogue
     - header-str: String, header-text
     - header-lines: Int, number of header-lines treated as sticky-header
     - header-first: Bool, print header before the prompt line
   - height: String, height of the fzf input dialogue
   - tac: Bool, reverse the order of the input
   - case-insensitive: Bool, toggle case-insensitive search (default: smart-case)
   - exact: Bool, toggle exact search (default: fuzzy)

   `args`: Input arguments to fzf (optional, list of strings)

   Examples:

   (fzf) ;; => \"myfile.txt\"

   (->> [\"quick\" \"brown\" \"fox\"]
       (map clojure.string/upper-case)
       fzf) ;; => \"FOX\"


   (fzf {:multi true
         :reverse true}
       [\"one \" \"two \" \"three \"]) ;; => [\"one\" \"two\"]


   Returns:
   - on success with :multi = false (default): the selected item as string
   - on success with :multi = true: the selected item(s) as vector of strings
   - on error or ctrl-c: nil"
  ([] (fzf {} []))
  ([opts-or-args]
   (if (map? opts-or-args)
     (fzf opts-or-args [])
     (fzf {} opts-or-args)))
  ([opts args]
   {:pre [(and (s/valid? :fzf/opts opts)
               (s/valid? :fzf/args args))]}
   (i/fzf opts args)))


(comment
  (s/valid? :fzf/opts {"a" "b"}))
