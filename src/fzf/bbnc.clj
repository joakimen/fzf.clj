(ns fzf.bbnc
  (:import (java.io BufferedInputStream BufferedOutputStream BufferedReader InputStreamReader PrintWriter)
           (java.net Socket)
           (java.nio.charset StandardCharsets)))

(defn -main [& args]
  (when (= 2 (count args))
    (let [[port selected] args]
      (with-open [sock (Socket. "127.0.0.1" (Integer/valueOf ^String port 10))]
        (with-open [in (BufferedReader. (InputStreamReader. (BufferedInputStream. (.getInputStream sock)) StandardCharsets/UTF_8))
                    out (PrintWriter. (BufferedOutputStream. (.getOutputStream sock)) true StandardCharsets/UTF_8)]
          (.println out (str selected))
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
