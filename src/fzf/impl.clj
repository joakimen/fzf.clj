(ns fzf.impl
  "Implementation of fzf-wrapper"
  (:require [babashka.process :as p]
            [babashka.fs :as fs]
            [clojure.string :as str]))

(defn parse-opts
  "Parse fzf and process options"
  ([] (parse-opts {}))
  ([opts]
   (let [{:keys [in dir multi preview reverse]} opts]
     {:cmd (cond-> ["fzf"]
             multi (conj "--multi")
             reverse (conj "--reverse")
             preview (conj "--preview" preview))
      :opts (cond-> {:in :inherit
                     :out :string
                     :err :inherit}
              in (assoc :in (str/join "\n" in))
              dir (assoc :dir (-> dir fs/expand-home str)))})))

(defn fzf
  "Internal interface to fzf"
  ([] (fzf {}))
  ([opts] (let [multi (:multi opts)
                {:keys [cmd opts]} (parse-opts opts)
                {:keys [out exit]} @(p/process cmd opts)]
            (if (zero? exit)
              (cond-> (str/trim out)
                multi str/split-lines)
              nil))))
