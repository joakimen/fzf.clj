# fzf.clj

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg?style=flat)](https://choosealicense.com/licenses/mit/) [![CI](https://github.com/joakimen/fzf.clj/actions/workflows/ci.yml/badge.svg)](https://github.com/joakimen/fzf.clj/actions/workflows/ci.yml) [![bb compatible](https://raw.githubusercontent.com/babashka/babashka/master/logo/badge.svg)](https://babashka.org)

A small wrapper around [fzf](https://github.com/junegunn/fzf).

Intended to be used in scripts in which the user wants to select one or more items from a selection using fuzzy-completion.

## Requirements

- [fzf](https://github.com/junegunn/fzf)
- Babashka or Clojure

## Usage

Add the following to your `bb.edn` or `deps.edn`:

```edn
{:deps {io.github.joakimen/fzf.clj {:git/sha "2063e0f6e1a7f78b5869ef1424e04e21ec46e1eb"}}}
```

### Example usage

#### Import

```clojure
(require '[fzf.core :refer [fzf]])
```

#### Single selection

```clojure
(fzf)
;; user selects an item
"src/bbfile/core.clj"
```

#### Multi-selection

```clojure
(fzf {:multi true})
;; user selects one or more items
["src/bbfile/core.clj" "src/bbfile/cli.clj" "deps.edn"]
```

#### Passing data to stdin

> :bulb: Since `v0.0.2`, input-arguments have been moved from the `in`-key in the options map to its own argument to facilitate threading.

To provide input arguments to fzf via STDIN instead of using the default file selection command, the arguments can be passed as a vector of strings.

```clojure
(fzf ["one" "two" "three"])
;; user selects an item
"two"
```

Threading

```clojure
(->> ["quick" "brown" "fox"] fzf) ;; => fox
```

Threading w/options

```clojure
(->> ["quick" "brown" "fox"]
     (map upper-case)
     (fzf {:multi true})) ;; => ["QUICK" "FOX"]
```

### Available options

[fzf](https://github.com/junegunn/fzf) supports a slew of options, so I have only included the ones I frequently use myself.

Key features include:
- The `:preview` string argument or a `:preview-fn (fn [selections])` that
  receives a vector of all selected items (via fzf's `{+}`).
- A key binding system offering:
  - `:command-bindings`: Define complex, multi-step actions triggered by
    key-chords. fzf actions generally take either a simple string argument
    (e.g., `change-prompt(...)`, `change-header(...)`) or an external
    command (e.g., `execute(...)`, `reload(...)`, `preview(...)`,
    `transform(...)`). You can specify these arguments in your action maps
    using:
    - `:simple-arg`: For actions that take a direct string value (e.g.,
      `{:action-name "change-prompt" :simple-arg "New>"}`).
    - `:command-string`: For actions that execute a raw shell command (e.g.,
      `{:action-name "execute" :command-string "less {}"}`).
    - `:handler-fn`: For actions that execute a Clojure function, which is
      wrapped into an executable shell command (e.g., `'(fn [lines] ...)`).
      - **NOTE** The `:handler-fn` argument MUST be quoted (e.g., `'(fn [lines])`)
      as it's passed as a string for external execution, unlike `:bbnc-reload-fn`.
    - `:bbnc-reload-fn`: A `(fn [query & selections])` special case for
      `reload` actions, allowing in-process Clojure functions for dynamic
      updates that can access application state. The function takes two
      arguments:
      1. The current fzf query string (from fzf's `{q}` placeholder).
      2. The current fzf selection string (from fzf's `{+}` placeholder).
         This string contains the line currently under the cursor.
         If fzf is in multi-select mode (due to the `:multi true` option)
         and multiple items have been explicitly selected (e.g., using TAB),
         this string will contain all selected items, space-separated.
      The function must return a collection of new candidate strings.
      Example: `(fn [current-query & current-selection] ...)`
  - `:additional-bindings`: Use raw fzf binding strings for maximum
    flexibility (escape hatch).
  - This wrapper aims to support most of fzf's [available actions](https://man.archlinux.org/man/fzf.1.en#AVAILABLE_ACTIONS:).
    Consult the fzf documentation to determine if a specific action expects a
    simple string (typically use `:simple-arg`) or an external command
    (typically use `:command-string` or `:handler-fn`).

For all available options, see the docstring in [core.clj](src/fzf/core.clj).

For detailed usage examples, including demonstrations of various binding options, see [src/fzf/examples.clj](src/fzf/examples.clj).

### Note on REPL-usage

Since [fzf](https://github.com/junegunn/fzf) expects an interactive terminal in which the user nagivates and selects items, a REPL-connection using a non-interactive terminal (e.g. Calva's "Jack-In") will not render the fzf-prompt, and invocations will cause the editor to hang.

To display and work with the fzf-prompt (e.g. calling the `(fzf)`-function) using the REPL, the nrepl-session must be started from an interactive terminal, then connected to from the editor.

#### Example for Calva

Start the repl

```bash
$ bb nrepl 1669
Started nREPL server at 127.0.0.1:1669
For more info visit: https://book.babashka.org/#_nrepl
```

Connect to the REPL Server in VS Code

1. Run command `Calva: Connect to a running REPL Server, not in project`
2. Choose `babashka`
3. Choose `localhost:1669`

## License

Copyright © 2023- Joakim L. Engeset

Distributed under the MIT License. See LICENSE.
