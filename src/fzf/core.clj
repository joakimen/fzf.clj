(ns fzf.core
  "Public interface to the fzf-wrapper"
  (:require [fzf.impl :as i]))

(defn fzf
  "Public interface to fzf.
   
   Options map (all keys are optional):
   - in: Vec of args passed as input to fzf as stdin
   - dir: String indicating the startup-dir of the fzf-command
   - multi: Bool, toggles multi-select in fzf. If true, fzf returns a vector instead of string
   - preview: String, preview-command for the currently selected item
   - reverse: Bool, reverse the order of the fzf input dialogue
   - header: Map with sticky-header options for the fzf input dialogue
     - header-str: String, header-text
     - header-lines: Int, number of header-lines treated as sticky-header
     - header-first: Bool, print header before the prompt line
   - tac: Bool, reverse the order of the input
   - case-insensitive: Bool, toggle case-insensitive search (default: smart-case)
   - exact: Bool, toggle exact search (default: fuzzy)
   
   Examples:

   (fzf) ;; => \"myfile.txt\"

   (fzf :multi true 
        :in [\"one\" \"two\" \"three\"] 
        :reverse true) ;; => [\"one\" \"two\"]
   
   Returns:
   - on success with :multi = false (default): the selected item as string
   - on success with :multi = true: the selected item(s) as vector of strings
   - on error or ctrl-c: nil"
  ([] (i/fzf))
  ([opts] (i/fzf opts)))
