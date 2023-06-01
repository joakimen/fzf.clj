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
{:deps {io.github.joakimen/fzf.clj {:git/sha "67e81183fc82b7153a58cf056ad8349286e7ec8e"}}}
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

```clojure
(fzf {:in ["one" "two" "three"]})
;; user selects an item
"two"
```

### Available options

[fzf](https://github.com/junegunn/fzf) supports a slew of options, so I have only included the ones I frequently use myself.

For all available options, see [core.clj](src/fzf/core.clj)

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
