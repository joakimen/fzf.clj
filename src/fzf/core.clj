(ns fzf.core
  "Public interface to the fzf-wrapper"
  (:require [clojure.spec.alpha :as s]
            [fzf.impl :as i]))

(defn fzf
  "Public interface to fzf.
   
   `opts`: Options map (all keys are optional)
   - dir: String indicating the startup-dir of the fzf-command
   - multi: Bool, toggles multi-select in fzf. If true, fzf returns a vector instead of string
   - preview: String, preview-command for the currently selected item
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
   {:pre [(s/and (s/valid? :fzf/opts opts)
                 (s/valid? :fzf/args args))]}
   (i/fzf opts args)))
