(ns fzf.impl
  "Implementation of fzf-wrapper"
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.string :as str])
  (:import (java.io BufferedInputStream BufferedOutputStream BufferedReader ByteArrayOutputStream Closeable InputStreamReader PrintWriter)
           (java.net InetSocketAddress ServerSocket Socket)
           (java.nio.charset StandardCharsets)))

(def bbnc-script
  "A verbatim copy of the namespace bbnc.
  This script does the following:
  1. connect to the preview server,
  2. send the argument and
  3. print the response."
  "(ns fzf.bbnc\n  (:import (java.io BufferedInputStream BufferedOutputStream BufferedReader InputStreamReader PrintWriter)\n           (java.net Socket)\n           (java.nio.charset StandardCharsets)))\n\n(defn -main [& args]\n  (when (= 2 (count args))\n    (let [[port selected] args]\n      (with-open [sock (Socket. \"127.0.0.1\" (Integer/valueOf ^String port 10))]\n        (with-open [in (BufferedReader. (InputStreamReader. (BufferedInputStream. (.getInputStream sock)) StandardCharsets/UTF_8))\n                    out (PrintWriter. (BufferedOutputStream. (.getOutputStream sock)) true StandardCharsets/UTF_8)]\n          (.println out (str selected))\n          (.flush out)\n          (loop []\n            (when-let [lin (try (.readLine in)\n                                (catch Throwable _\n                                  nil))]\n              (println lin)\n              (flush)\n              (recur))))))))\n\n#_(when (= *file* (System/getProperty \"babashka.file\")))\n(apply -main *command-line-args*)\n")

(defn bbnc-preview-command [port]
  (str/join " " ["bb"
                 (str "-e '" bbnc-script "'")
                 (str port)
                 "{}"]))

(defn parse-opts
  "Parse fzf and process options"
  ([opts args]
   (parse-opts opts args nil))
  ([opts args server-port]
   (let [{:keys [dir multi preview-fn preview tac case-insensitive exact reverse height]
          {:keys [header-str header-lines header-first]} :header} opts]
     {:cmd (cond-> ["fzf"]
             multi (conj "--multi")
             reverse (conj "--reverse")
             tac (conj "--tac")
             case-insensitive (conj "-i")
             exact (conj "--exact")
             preview-fn (conj "--preview" (bbnc-preview-command server-port))
             preview (conj "--preview" preview)
             header-str (conj "--header" header-str)
             header-lines (conj "--header-lines" header-lines)
             header-first (conj "--header-first")
             height (conj "--height" height))
      :opts (cond-> {:in :inherit
                     :out :string
                     :err :inherit}
                    (not-empty args) (assoc :in (str/join "\n" args))
                    dir (assoc :dir (-> dir fs/expand-home str)))})))

(defn start-preview-fn-server
  "Start the preview function server.

  The fzf preview command, a bb script, will connect to this server,
  send the selected item and print the response.

  This server will produce the response by
  invoking `preview-fn` with the selected item as the only argument."
  ^Closeable [port-promise preview-fn]
  (let [ss (doto
             (ServerSocket.)
             (.bind (InetSocketAddress. "127.0.0.1" 0)))]
    (future
      (deliver port-promise (.getLocalPort ss))
      (loop []
        (when-let [^Socket sock (try
                                  (.accept ss)
                                  (catch Throwable _
                                    nil))]
          (try
            (with-open [in (BufferedReader. (InputStreamReader. (BufferedInputStream. (.getInputStream sock)) StandardCharsets/UTF_8))
                        out (PrintWriter. (BufferedOutputStream. (.getOutputStream sock)) true StandardCharsets/UTF_8)]
              (let [input (.readLine in)
                    response (str (preview-fn input))]
                (.println out ^String response)
                (.flush out)))
            (catch Throwable _
              nil)
            (finally
              (try
                (.close sock)
                (catch Throwable _
                  nil))))
          (recur))))
    ss))

(defn fzf
  "Internal interface to fzf"
  [{:keys [preview-fn] :as opts} args]
  (let [port-promise (promise)]
    (with-open [^Closeable _server (if preview-fn
                                     (start-preview-fn-server port-promise preview-fn)
                                     (ByteArrayOutputStream.))] ; any proper Closeable thing will do.
      (let [multi (:multi opts)
            {:keys [cmd opts]} (parse-opts opts args (when preview-fn @port-promise))
            {:keys [out exit]} @(p/process cmd opts)]
        (if (zero? exit)
          (cond-> (str/trim out)
                  multi str/split-lines)
          nil)))))
