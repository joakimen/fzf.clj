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
(s/def :fzf/throw boolean?)
(s/def :fzf/select-1 boolean?)
(s/def :fzf/query string?)

(s/def :fzf/binding-handler-fn (s/fspec :args (s/cat :query string? :current-selection string?) :ret (s/coll-of string?)))
(s/def :fzf/in-process-fn fn?)  ;; For in-process reload via bbnc, used within command-bindings

(s/def :fzf/action-name string?)      ;; e.g., "reload", "execute", "change-prompt"
(s/def :fzf/command-string string?)   ;; For execute(:command-string "...")
(s/def :fzf/handler-fn any?)          ;; Clojure form or string for the handler function. Receives lines from {+f} as a vector of strings.
(s/def :fzf/simple-arg string?)       ;; For actions like change-prompt(:simple-arg "foo")
(s/def :fzf/action-map
  (s/and
   (s/keys :req-un [:fzf/action-name]
           :opt-un [:fzf/command-string :fzf/handler-fn :fzf/simple-arg :fzf/in-process-fn])
   ;; Must have at most one of :command-string, :handler-fn, :simple-arg, :in-process-fn
   #(<= (count (filter % [:command-string :handler-fn :simple-arg :in-process-fn])) 1)
   ;; TODO: can probably extend to anythign else that takes an `external command` (see fzf man page)
   ;; If :fzf/in-process-fn is present, :fzf/action-name must be "reload", "execute" or "execute-silent"
   #(if (:in-process-fn %)
      (#{"reload" "execute" "execute-silent"} (:action-name %)) true)))

(s/def :fzf/action-spec (s/or :simple-action-name string?
                              :action-with-args :fzf/action-map))
(s/def :fzf/command-binding-actions (s/coll-of :fzf/action-spec :kind vector? :min-count 1))
(s/def :fzf/command-bindings (s/map-of string? :fzf/command-binding-actions))
(s/def :fzf/additional-bindings (s/coll-of string?))

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
                    :fzf/exact
                    :fzf/throw
                    :fzf/select-1
                    :fzf/query
                    :fzf/command-bindings ; For fzf action(command) or action(arg) or action
                    :fzf/additional-bindings]) ; For raw fzf binding strings
          #(not (and (:preview %) (:preview-fn %)))))

(s/def :fzf/args sequential?)

(defn fzf
  "Public interface to fzf.

   `opts`: Options map (all keys are optional)
   - dir: String indicating the startup-dir of the fzf-command
   - multi: Bool, toggles multi-select in fzf. If true, fzf returns a vector instead of string
   - preview: String, preview-command for the currently selected item (uses `{}` placeholder)
   - preview-fn: Function, preview function that will be called with a vector of strings,
                 where each string is a currently selected item (uses `{+}` placeholder). Its return value will be
                 displayed in the preview window.
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
   - throw: Bool, throw when no candidates were selected (default: return nil)
   - select-1: Bool, automatically select if only one match
   - query: String, start finder with the specified query

   - command-bindings: Map for defining fzf bindings that mirror fzf's own binding syntax,
                       including action composition (chaining actions with '+').
                       Keys are fzf key-chords. Values are vectors of action specifications.
                       Each action spec in the vector can be:
                         - A string (for actions without arguments, e.g., `\"accept\"`).
                         - A map (for actions with arguments):
                           - `:action-name` (string, mandatory): The fzf action (e.g., \"reload\", \"execute\").
                           - One of the following for the argument:
                             - `:simple-arg` (string): For simple string arguments (e.g., `{:action-name \"change-prompt\" :simple-arg \"New>\"}`).
                             - `:command-string` (string): A raw command string (e.g., `{:action-name \"execute\" :command-string \"less {}\"}`).
                             - `:handler-fn` (a **pure, quoted** `'(fn [])`): A Clojure function.
                               It receives one argument: a vector of strings (lines from fzf's `{+f}` file).
                               It should return:
                                 - A single string (for actions like `transform-header`).
                                 - A collection of strings (for actions like `reload`).
                               The returned value is printed to stdout (collections are newline-joined).
                             - `:in-process-fn` (Clojure function): For `reload` actions that require in-process execution
                               (access to the parent application's state). The function takes:
                                 1. The current fzf query string (from fzf's `{q}` placeholder).
                                 2. The current fzf selection string (from fzf's `{+}` placeholder).
                                    This string contains the line currently under the cursor.
                                    If fzf is in multi-select mode (due to the `:multi true` option)
                                    and multiple items have been explicitly selected (e.g., using TAB),
                                    this string will contain all selected items, space-separated.
                               And must return a collection of new candidate strings.
                               Must be used with `:action-name \"reload\"`.
                       Example:
                       `{:command-bindings {\"ctrl-s\" [{:action-name \"change-prompt\" :simple-arg \"Saving...\"}
                                                       {:action-name \"execute\" :handler-fn '(fn [lines] (spit \"/tmp/out.txt\" (clojure.string/join \"\\n\" lines)))}
                                                       \"accept\"],
                                             \"ctrl-r\" [{:action-name \"reload\" :in-process-fn (fn [q] (filter #(str/includes? % q) [\"item1\" \"item2\"]))}]}}`
   - additional-bindings: A collection of raw fzf binding strings (e.g., `\"ctrl-x:accept+execute(ls)\"`).
                          Useful for complex bindings or actions not covered by `:command-bindings`.

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
