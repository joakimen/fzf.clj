(ns fzf.impl
  "Implementation of fzf-wrapper"
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.string :as str]))

(defn parse-opts
  "Parse fzf and process options"
  [opts args]
  (let [{:keys [dir multi preview tac case-insensitive exact reverse height]
         {:keys [header-str header-lines header-first]} :header} opts]
    {:cmd (cond-> ["fzf"]
            multi (conj "--multi")
            reverse (conj "--reverse")
            tac (conj "--tac")
            case-insensitive (conj "-i")
            exact (conj "--exact")
            preview (conj "--preview" preview)
            header-str (conj "--header" header-str)
            header-lines (conj "--header-lines" header-lines)
            header-first (conj "--header-first")
            height (conj "--height" height))
     :opts (cond-> {:in :inherit
                    :out :string
                    :err :inherit}
             (not-empty args) (assoc :in (str/join "\n" args))
             dir (assoc :dir (-> dir fs/expand-home str)))}))

(defn fzf
  "Internal interface to fzf"
  [opts args]
  (let [multi (:multi opts)
        {:keys [cmd opts]} (parse-opts opts args)
        {:keys [out exit]} @(p/process cmd opts)]
    (if (zero? exit)
      (cond-> (str/trim out)
        multi str/split-lines)
      nil)))
