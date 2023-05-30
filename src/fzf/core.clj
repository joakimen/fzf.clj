(ns fzf.core
  "Public interface to the fzf-wrapper"
  (:require [fzf.impl :as i]))

(defn fzf
  "Public interface to fzf.
   
   Options map (all keys are optional):
   in: Vec of args passed as input to fzf as stdin
   dir: String indicating the startup-dir of the fzf-command
   multi: Bool, that toggles multi-select in fzf. If true, fzf returns a vector instead of string
   preview: String, contains a preview-command passed to fzf
   reverse: Bool, reverses the order of the fzf input dialogue
   
   Examples:

   (fzf) ;; => \"myfile.txt\"

   (fzf :multi true 
        :in [\"one\" \"two\" \"three\"] 
        :reverse true) ;; => [\"one\" \"two\"]
   
   Returns:
   - on success with :multi = false: the selected item as string
   - on success with :multi = true: the selected item(s) as vector of strings
   - on error or ctrl-c: nil"
  ([] (i/fzf))
  ([opts] (i/fzf opts)))
