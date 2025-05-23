(ns fzf.bbnc
  (:require [clojure.string])
  (:import (java.io BufferedInputStream BufferedOutputStream BufferedReader InputStreamReader PrintWriter)
           (java.net Socket)
           (java.nio.charset StandardCharsets)))

(defn -main [& args]
  (when (>= (count args) 2)
    (let [port (first args)
          type (second args)]
      (with-open [sock (Socket. "127.0.0.1" (Integer/valueOf ^String port 10))]
        (with-open [in (BufferedReader. (InputStreamReader. (BufferedInputStream. (.getInputStream sock)) StandardCharsets/UTF_8))
                    out (PrintWriter. (BufferedOutputStream. (.getOutputStream sock)) true StandardCharsets/UTF_8)]
          (.println out type)
          (condp = type
            "preview"
            (let [selected-items-coll (nnext args)
                  selected-str (clojure.string/join " " selected-items-coll)]
              (.println out selected-str))

            "binding"
            (let [binding-id (nth args 2)
                  query-str (nth args 3 "")
                  ;; Collect all subsequent arguments as parts of the selection,
                  ;; as {+} can expand to multiple space-separated items which become separate args.
                  ;; TODO: Consider using {+f} instead to accomodate for space-separated items.
                  selection-parts (if (> (count args) 4)
                                    (subvec (vec args) 4)
                                    [""]) ; Default to a vector with an empty string if no selection parts
                  current-selection-str (clojure.string/join " " selection-parts)]
              (.println out binding-id)
              (.println out query-str)
              (.println out current-selection-str)))
          (.flush out)
          (loop []
            (when-let [lin (try (.readLine in)
                                (catch Throwable _
                                  nil))]
              (println lin)
              (flush)
              (recur))))))))

#_(when (= *file* (System/getProperty "babashka.file")))
(apply -main *command-line-args*)
