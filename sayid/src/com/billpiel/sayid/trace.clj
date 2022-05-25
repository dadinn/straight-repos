(ns com.billpiel.sayid.trace
  (:require [com.billpiel.sayid.util.other :as util])
  (:import com.billpiel.sayid.SayidMultiFn))

(def ^:dynamic *trace-log-parent* nil)

(defn now [] (System/currentTimeMillis))

(defn mk-tree
  [& {:keys [id-prefix parent]}]
  (let [id (-> id-prefix
               gensym
               keyword)
        path (conj (or (:path parent)
                       [])
                   id)]
    ^::tree {:id id
             :path path
             :depth (or (some-> parent :depth inc) 0)
             :children (atom [])}))

(defn mk-fn-tree
  [& {:keys [parent name args meta]}]
  (assoc  (mk-tree :parent parent) ;; 3 sec
          :name name
          :args (vec args)
          :meta meta
          :arg-map (delay (util/arg-match-safe (-> meta
                                              :arglists
                                              vec)
                                          args))
          :started-at (now)))

(defn StackTraceElement->map
  [^StackTraceElement o]
  {:class-name (.getClassName o)
   :file-name (.getFileName o)
   :method-name (.getMethodName o)
   :line-number (.getLineNumber o)})

(defn Throwable->map**
  "Constructs a data representation for a Throwable."
  {:added "1.7"}
  [^Throwable o]
  (let [base (fn [^Throwable t]
               (let [m {:type (class t)
                        :message (.getLocalizedMessage t)
                        :at (StackTraceElement->map (get (.getStackTrace t) 0))}
                     data (ex-data t)]
                 (if data
                   (assoc m :data data)
                   m)))
        via (loop [via [], ^Throwable t o]
              (if t
                (recur (conj via t) (.getCause t))
                via))
        ^Throwable root (peek via)
        m {:cause (.getLocalizedMessage root)
           :via (vec (map base via))
           :trace (mapv StackTraceElement->map (.getStackTrace ^Throwable (or root o)))}
        data (ex-data root)]
    (if data
      (assoc m :data data)
      m)))


(defn start-trace
  [trace-log tree]
  (swap! trace-log
         conj
         tree))  ;; string!!

(defn end-trace
  [trace-log idx tree]
  (swap! trace-log
         update-in
         [idx]
         #(merge % tree)))

(defn trace-fn-call
  [workspace name f args meta']
  (let [parent (or *trace-log-parent*
                   workspace)
        this  (mk-fn-tree :parent parent ;; mk-fn-tree = 200ms
                          :name name
                          :args args
                          :meta meta')
        idx  (-> (start-trace (:children parent) ;; start-trace = 20ms
                              this)
                 count
                 dec)]
    (let [value (binding [*trace-log-parent* this] ;; binding = 50ms
                  (try
                    (apply f args)
                    (catch Throwable t
                      (end-trace (:children parent)
                                 idx
                                 {:throw (Throwable->map** t)
                                  :ended-at (now)})
                      (throw t))))]
      (end-trace (:children parent) ;; end-trace = 75ms
                 idx
                 {:return value
                  :ended-at (now)})
      value)))

(defn shallow-tracer-multifn
  [{:keys [workspace qual-sym meta']} original-fn]
  (com.billpiel.sayid.SayidMultiFn. {:original original-fn
                                     :trace-dispatch-fn (fn [f args]
                                                          (trace-fn-call workspace
                                                                         (symbol (str qual-sym "--DISPATCHER"))
                                                                         f
                                                                         args
                                                                         meta'))
                                     :trace-method-fn (fn [f args]
                                                        (trace-fn-call workspace
                                                                       qual-sym
                                                                       f
                                                                       args
                                                                       meta'))}))

(defn ^{::trace-type :fn} shallow-tracer
  [{:keys [workspace qual-sym meta'] :as m} original-fn]
  (if (= (type original-fn) clojure.lang.MultiFn)
    (shallow-tracer-multifn m original-fn)
    (fn tracing-wrapper [& args]
      (trace-fn-call workspace
                     qual-sym
                     original-fn
                     args
                     meta'))))

(defn apply-trace-to-var
  [^clojure.lang.Var v tracer-fn workspace]
  (let [ns (.ns v)
        s  (.sym v)
        m (meta v)
        f @v
        vname (util/qualify-sym ns s )]
    (doto v
      (alter-var-root (partial tracer-fn
                               {:workspace workspace
                                :ns' ns
                                :sym s
                                :qual-sym vname
                                :meta' m}))
      (alter-meta! assoc ::traced [(:id workspace) f])
      (alter-meta! assoc ::trace-type (-> tracer-fn meta ::trace-type)))))

(defn untrace-var*
  ([ns s]
   (untrace-var* (ns-resolve ns s)))
  ([v]
   (let [^clojure.lang.Var v (if (var? v) v (resolve v))
         ns (.ns v)
         s  (.sym v)
         [_ f]  ((meta v) ::traced)]
     (when f
       (doto v
         (alter-var-root (constantly f))
         (alter-meta! dissoc
                      ::traced
                      ::trace-type))))))

(defn trace-var*
  [v tracer-fn workspace & {:keys [no-overwrite]}]
  (let [^clojure.lang.Var v (if (var? v) v (resolve v))]
    (when (and (ifn? @v) (-> v meta :macro not))
      (if-let [[traced-id traced-f] (-> v meta ::traced)]
        (when (and (not no-overwrite)
                   (or (not= traced-id (:id workspace))
                       (not= (-> tracer-fn meta ::trace-type)
                             (-> v meta ::trace-type))))
          (untrace-var* v)
          (apply-trace-to-var v tracer-fn workspace))
        (apply-trace-to-var v tracer-fn workspace)))))

(defn the-ns-safe
  [ns]
  (try (the-ns ns)
       (catch Exception e
         nil)))

(defn trace-ns*
  [ns workspace]
  (when-let [ns (the-ns-safe ns)]
    (when-not ('#{clojure.core com.billpiel.sayid.core} (.name ns))
      (let [ns-fns (->> ns ns-interns vals (filter (comp util/fn*? var-get)))]
        (doseq [f ns-fns]
          (trace-var* f
                      (util/assoc-var-meta-to-fn shallow-tracer
                                                 ::trace-type)
                      workspace
                      :no-overwrite true))))))

(defn untrace-ns*
  [ns*]
  (when-let [ns' (the-ns-safe ns*)]
    (let [ns-fns (->> ns' ns-interns vals)]
      (doseq [f ns-fns]
        (untrace-var* f)))))

(defn apply->vec
  [f]
  (fn [v] [v (f v)]))

(defn audit-fn
  [fn-var trace-selection]
  (let [fn-meta (meta fn-var)]
    (-> fn-meta
        (update-in [:ns] str)
        (assoc :trace-type (::trace-type fn-meta)
               :trace-selection trace-selection)
        (dissoc ::trace-type
                ::traced))))

(defn audit-fn-sym
  [fn-sym trace-selection]
  (-> fn-sym
      resolve
      (audit-fn trace-selection)))

(defn audit-ns
  [ns-sym]
  (try (let [mk-vec-fn (fn [fn-var]
                         [(-> fn-var meta :name)
                          (audit-fn fn-var :ns)])]
         (->> ns-sym
              ns-interns
              vals
              (filter (comp fn? var-get))
              (map mk-vec-fn)
              (into (sorted-map))))
       (catch Exception ex
         (sorted-map))))

(defn audit-traces
  [traced]
  (let [{outer :fn inner :inner-fn ns' :ns} traced
        f (fn [trace-type]
            (fn [fn-sym]
              (let [fn-var (resolve fn-sym)]
                [(-> fn-var meta :name)
                 (audit-fn fn-var trace-type)])))
        fn-audits (->> (concat (map (f :fn) outer)
                               (map (f :inner-fn) inner))
                       (group-by #(-> % second :ns))
                       (map (fn [[k v]] [(symbol k) (into (sorted-map) v)]))
                       (into (sorted-map)))]
    {:ns (into (sorted-map)
               (map (apply->vec audit-ns)
                    ns'))
     :fn fn-audits}))

(defn check-fn-trace-type
  [fn-sym]
  (-> fn-sym
      resolve
      meta
      ::trace-type))

(defmulti trace* (fn [type sym workspace] type))

(defmethod trace* :ns
  [_ sym workspace]
  (trace-ns* sym workspace))



(defmethod trace* :fn
  [_ fn-sym workspace]
  (-> fn-sym
      resolve
      (trace-var* (util/assoc-var-meta-to-fn shallow-tracer
                                        ::trace-type)
                  workspace)))

(defmulti untrace* (fn [type sym] type))

(defmethod untrace* :ns
  [_ sym]
  (untrace-ns* sym))

(defmethod untrace* :fn
  [_ fn-sym]
  (-> fn-sym
      resolve
      untrace-var*))
