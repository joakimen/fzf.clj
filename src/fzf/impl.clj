(ns fzf.impl
  "Implementation of fzf-wrapper"
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io
    BufferedInputStream
    BufferedOutputStream
    BufferedReader
    ByteArrayOutputStream
    Closeable
    InputStreamReader
    PrintWriter)
   (java.net InetSocketAddress ServerSocket Socket)
   (java.nio.charset StandardCharsets)))

(defonce bbnc-script-content (slurp (io/resource "fzf/bbnc.clj")))

(defn bbnc-preview-command [bbnc-script-path-str port]
  (str/join " " ["bb"
                 (str "\"" bbnc-script-path-str "\"")
                 (str port)
                 "preview"
                 "{+}"]))

(defn bbnc-binding-command-str [bbnc-script-path-str port binding-id]
  (str/join " " ["bb"
                 (str "\"" bbnc-script-path-str "\"")
                 (str port)
                 "binding"
                 binding-id
                 "{q}"
                 ;; current selection(s)
                 "{+}"]))

(defn build-babashka-eval-string [handler-fn-form]
  (let [;; Ensure handler-fn-form is a Clojure form (symbol, list, etc.)
        ;; If it's a string of code, parse it.
        handler-as-form (if (string? handler-fn-form)
                          (read-string {:read-cond :allow} handler-fn-form)
                          handler-fn-form)]
    ;; The user's handler-fn is called with lines from {+f}.
    ;; Its result is then printed to stdout. Collections are joined by newlines.
    (pr-str
      `(let [result# (~handler-as-form (clojure.string/split-lines (slurp "{+f}")))]
         (if (coll? result#)
           (print (clojure.string/join "\n" result#))
           (print result#))
         (flush)))))

(defn create-babashka-script-file*
  "Creates a temporary script file with the given Clojure code.
   The file is marked for deletion on JVM exit.
   Returns the absolute path string of the created file."
  [clj-code-form-or-string prefix-str]
  (let [content-string (if (string? clj-code-form-or-string)
                         clj-code-form-or-string
                         (pr-str clj-code-form-or-string))
        temp-file (fs/create-temp-file {:prefix prefix-str
                                        :suffix ".clj"})
        temp-file-path-str (-> temp-file fs/absolutize str)]
    (spit temp-file-path-str content-string)
    temp-file-path-str))

(defn prepare-babashka-script-command
  "Takes Clojure code (form or string), creates a temp script,
  and returns a command string like 'bb \"/abs/path/to/script.clj\"'."
  [clj-code-form-or-string]
  (let [abs-path-str (create-babashka-script-file* clj-code-form-or-string "fzf-bb-")
        cmd (str "bb \"" abs-path-str "\"")]
    cmd))

(defn format-action-spec
  "Formats an action specification (string or map) into a string for fzf --bind."
  [action-spec]
  (if (string? action-spec)
    action-spec
    (let [{:keys [action-name command-string handler-fn simple-arg]} action-spec]
      (cond
        simple-arg (str action-name "(" simple-arg ")")
        command-string (str action-name "(" command-string ")")
        handler-fn (let [bb-eval-str (build-babashka-eval-string handler-fn)
                         ;; TODO: This single quote escaping is very basic and might not cover all edge cases.
                         escaped-bb-eval-str (str/replace bb-eval-str "'" "'\\''")]
                     (str action-name "(bb -e '" escaped-bb-eval-str "')"))
        ; Action map with no arg, e.g. {:action-name "clear-query"}
        (some? action-name) action-name
        :else (throw (ex-info "Invalid action specification in :command-bindings"
                              {:action-spec action-spec}))))))

(defn build-fzf-base-command
  "Builds the base fzf command vector from options."
  [opts server-port bbnc-script-path]
  (let [{:keys [multi preview-fn preview tac case-insensitive exact reverse height select-1 query
                additional-raw-args]
         {:keys [header-str header-lines header-first]} :header} opts
        preview-command-str (if (and preview-fn bbnc-script-path server-port)
                              (bbnc-preview-command bbnc-script-path server-port)
                              ; Fallback to literal :preview string
                              preview)]
    (cond-> ["fzf"]
      multi (conj "--multi")
      reverse (conj "--reverse")
      tac (conj "--tac")
      case-insensitive (conj "-i")
      exact (conj "--exact")
      preview-command-str (conj "--preview" preview-command-str)
      header-str (conj "--header" header-str)
      header-lines (conj "--header-lines" (str header-lines))
      header-first (conj "--header-first")
      height (conj "--height" (str height))
      select-1 (conj "--select-1")
      query (conj "--query" query)
      additional-raw-args (into additional-raw-args))))

(defn add-additional-bindings-to-command
  "Adds --bind options from :additional-bindings to the command vector."
  [cmd-vec additional-bindings-vec]
  (reduce (fn [cmd binding-str]
            (-> cmd (conj "--bind") (conj binding-str)))
          cmd-vec
          additional-bindings-vec))

(defn add-command-bindings-to-command
  "Adds --bind options from :command-bindings to the command vector."
  [cmd-vec command-bindings-map]
  (reduce-kv (fn [cmd key-chord action-specs-vec]
               (let [action-parts (mapv (fn [action-spec]
                                          (try
                                            (format-action-spec action-spec)
                                            (catch Exception e
                                              (throw (ex-info (str "Error formatting action spec for key: " key-chord)
                                                              {:key key-chord :action-spec action-spec} e)))))
                                        action-specs-vec)
                     full-binding-str (str key-chord ":" (str/join "+" action-parts))]
                 (-> cmd (conj "--bind") (conj full-binding-str))))
             cmd-vec
             command-bindings-map))

(defn build-process-options
  "Builds the options map for babashka.process."
  [dir args]
  (cond-> {:in :inherit
           :out :string
           :err :inherit}
    (not-empty args) (assoc :in (str/join "\n" args))
    dir (assoc :dir (-> dir fs/expand-home str))))

(defn parse-opts
  "Parse fzf and process options into a command vector and process options map."
  [opts args server-port bbnc-script-path]
  (let [{:keys [dir additional-bindings command-bindings]} opts
        final-cmd (-> (build-fzf-base-command opts server-port bbnc-script-path)
                      (add-additional-bindings-to-command (or additional-bindings []))
                      (add-command-bindings-to-command (or command-bindings {})))
        process-opts (build-process-options dir args)]
    {:cmd final-cmd
     :opts process-opts}))

(defn start-comms-server
  "Start the communications server for fzf.

  This server handles requests from `bbnc.clj` for both preview
  and custom bindings. `bbnc.clj` connects, sends a type identifier
  (\"preview\" or \"binding\"), and then type-specific data.

  For \"preview\":
  - `bbnc.clj` sends selected item(s).
  - Server calls `preview-fn` with these items.
  - Server sends `preview-fn`'s string output back.

  For \"binding\":
  - `bbnc.clj` sends `binding-id` and current fzf query.
  - Server calls handler from `binding-handlers` map using `binding-id`.
  - Handler returns a list of new candidates.
  - Server sends newline-separated candidates back.
  "
  ^Closeable [port-promise preview-fn binding-handlers]
  (let [ss (doto
             (ServerSocket.)
             (.bind (InetSocketAddress. "127.0.0.1" 0)))
        actual-port (.getLocalPort ss)]
    (future
      (deliver port-promise actual-port)
      (loop [conn-idx 0]
        (when-let [^Socket sock (try (.accept ss) (catch Throwable _ nil))]
          (try
            (with-open [in (BufferedReader. (InputStreamReader. (BufferedInputStream. (.getInputStream sock)) StandardCharsets/UTF_8))
                        out (PrintWriter. (BufferedOutputStream. (.getOutputStream sock)) true StandardCharsets/UTF_8)]
              (let [type (.readLine in)]
                (condp = type
                  "preview"
                  (when preview-fn
                    (let [input-line (.readLine in)
                          selected-items (if (nil? input-line)
                                           []
                                           (str/split input-line #"\s+"))
                          response (str (preview-fn selected-items))]
                      (.println out ^String response)))

                  "binding"
                  (when binding-handlers
                    (let [binding-id (.readLine in)
                          query-str (.readLine in)
                          current-selection-str (.readLine in)
                          handler-fn (get binding-handlers binding-id)]
                      (when handler-fn
                        (let [new-candidates (handler-fn query-str current-selection-str)
                              response (str/join "\n" new-candidates)]
                          (.println out ^String response))))))
                (.flush out)))
            (catch Throwable e
              (.println System/err (str "Error in comms server: " (.getMessage e) (when-let [data (ex-data e)] (str " " data)))))
            (finally
              (try (.close sock) (catch Throwable _ nil))))
          (recur (inc conn-idx)))))
    ss))

(defn transform-bindings-for-bbnc-registration
  "Processes command-bindings to register :in-process-fn handlers
  and replace them with a temporary :__bbnc_id__."
  [command-bindings register-bbnc-handler-fn]
  (reduce-kv
    (fn [m key-chord actions-vec]
      (assoc m key-chord
             (mapv (fn [action-spec]
                     (if (and (map? action-spec) (:in-process-fn action-spec))
                       (let [handler (:in-process-fn action-spec)
                             unique-id (register-bbnc-handler-fn handler)]
                         (assoc (dissoc action-spec :in-process-fn) :__bbnc_id__ unique-id))
                       action-spec))
                   actions-vec)))
    {}
    command-bindings))

(defn transform-bindings-with-resolved-bbnc-commands
  "Processes command-bindings to replace :__bbnc_id__ with
  a full :command-string for bbnc execution."
  [command-bindings-with-ids bbnc-script-path server-port]
  (reduce-kv
    (fn [m key-chord actions-vec]
      (assoc m key-chord
             (mapv (fn [action-spec]
                     (if (and (map? action-spec) (:__bbnc_id__ action-spec))
                       (let [bbnc-id (:__bbnc_id__ action-spec)
                             cmd-str (bbnc-binding-command-str bbnc-script-path server-port bbnc-id)]
                         (assoc (dissoc action-spec :__bbnc_id__) :command-string cmd-str))
                       action-spec))
                   actions-vec)))
    {}
    command-bindings-with-ids))

(defn prepare-bbnc-integration
  "Sets up bbnc handlers and performs the first pass of options transformation
  for :in-process-fn.
  Returns a map containing :opts-with-temp-ids (options with temporary bbnc IDs)
  and :final-binding-handlers (map of registered bbnc handlers)."
  [original-opts]
  (let [collected-bbnc-handlers-map (atom {})
        next-bbnc-id (atom 0)
        register-bbnc-handler! (fn [handler-fn]
                                 (let [id (str "bbnc-handler-" (swap! next-bbnc-id inc))]
                                   (swap! collected-bbnc-handlers-map assoc id handler-fn)
                                   id))

        opts-with-temp-ids (if-let [current-command-bindings (:command-bindings original-opts)]
                             (assoc original-opts :command-bindings
                                    (transform-bindings-for-bbnc-registration
                                      current-command-bindings
                                      register-bbnc-handler!))
                             original-opts)]
    {:opts-with-temp-ids opts-with-temp-ids
     :final-binding-handlers @collected-bbnc-handlers-map}))

(defn fzf
  "Internal interface to fzf"
  [{:keys [preview-fn] :as initial-opts} args]
  (let [port-promise (promise)
        ;; Prepare bbnc integration (handler registration and first pass opts transformation)
        bbnc-prep-result (prepare-bbnc-integration initial-opts)
        opts-with-temp-ids (:opts-with-temp-ids bbnc-prep-result)
        final-binding-handlers-for-server (:final-binding-handlers bbnc-prep-result)

        ;; Determine if server is needed based on preview-fn or registered bbnc handlers
        server-active (or preview-fn (not-empty final-binding-handlers-for-server))

        bbnc-script-path (when server-active (create-babashka-script-file* bbnc-script-content "fzf-bbnc-"))]

      (with-open [^Closeable _server (if server-active
                                       (start-comms-server port-promise preview-fn final-binding-handlers-for-server)
                                       (ByteArrayOutputStream.))] ; any proper Closeable thing will do.
        (let [server-port (when server-active @port-promise)
              ;; Second pass for :command-bindings: replace :__bbnc_id__ with :command-string
              ;; Check if :command-bindings key exists and server is active
              final-opts (if (and server-active (:command-bindings opts-with-temp-ids))
                           (assoc opts-with-temp-ids :command-bindings
                                  (transform-bindings-with-resolved-bbnc-commands
                                    (:command-bindings opts-with-temp-ids)
                                    bbnc-script-path
                                    server-port))
                           opts-with-temp-ids)

              multi (:multi final-opts)
              throw-on-nzec (:throw final-opts)
              {:keys [cmd opts]} (parse-opts final-opts args server-port bbnc-script-path)
              {:keys [out exit]} @(p/process cmd opts)]
          (if (zero? exit)
            (cond-> (str/trim out)
              multi str/split-lines)
            (if throw-on-nzec (throw (ex-info "No candidates were selected" {:babashka/exit 1}))
              nil))))))
