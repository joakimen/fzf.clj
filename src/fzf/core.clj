(ns fzf.core
  (:require [fzf.impl :as i]))

(defn fzf
  "Public interface to fzf"
  []
  (i/fzf))
