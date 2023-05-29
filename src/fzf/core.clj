(ns fzf.core
  (:require [fzf.impl :as i]))

defn fzf
"The public interface to fzf"
[]
(i/fzf)
