(ns mississippi.core)

(defn add-error [subject attr error]
  (assoc-in subject [:errors attr]
            (conj (get-in subject [:errors attr] []) error)))

(defn safe-parse-int [s]
  (try
    (Integer. s)
    (catch NumberFormatException e
      nil)))

(defn required [subject attr]
  (if (subject attr)
    subject
    (add-error subject attr "required")))

(defn numeric [subject attr]
  (if-let [val (safe-parse-int (subject attr))]
    (assoc subject attr val)
    (add-error subject attr "non numeric")))

(defn member-of
  [lat]
  (fn [subject attr]
    (if (contains? lat (subject attr))
      subject
      (add-error subject
                 attr
                 (format "is not a member of %s"
                         (apply str
                                (interpose ", " lat)))))))

(defn in-range
  [r]
  (fn [subject attr]
    (-> (numeric subject attr) 
        ((member-of (set r)) attr))))

(defn validate
  [subject validations]
  (reduce (fn [acc [attr func]]
            (if (vector? func)
              (reduce (fn [acc2 f] (f acc2 attr)) acc func)
              (func acc attr))) subject validations))


