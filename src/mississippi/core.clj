(ns mississippi.core
  (:require [clojure.walk :as walk]))

(defn add-error [subject attr error]
  (let [attr-path (apply vector :errors attr)]
    (assoc-in subject attr-path
              (conj (get-in subject attr-path []) error))))

(defn safe-parse-int [s]
  (try
    (Integer. s)
    (catch NumberFormatException e
      nil)))

(defn #^{:validation true} required
  ([subject attr]
     (required subject attr "required"))
  ([subject attr message]
     (if (get-in subject attr)
       subject
       (add-error subject attr message))))

(defn #^{:validation true} numeric
  ([subject attr] (numeric subject attr "non numeric"))
  ([subject attr message]
     (if-let [val (safe-parse-int (get-in subject attr))]
       (assoc-in subject attr val)
       (add-error subject attr message))))

(defn #^{:validation true} member-of
  ([lat] (member-of lat (format "is not a member of %s"
                                (apply str
                                       (interpose ", " lat)))))
  ([lat message]
     (fn [subject attr]
       (if (contains? lat (get-in subject attr))
         subject
         (add-error subject attr message)))))

(defn #^{:validation true} in-range
  ([r] (in-range r nil))
  ([r message]
     (fn [subject attr]
       (if message
         ((member-of (set r) message) (numeric subject attr) attr)
         ((member-of (set r)) (numeric subject attr) attr)))))

(defn validate-attr [subject [attr v-funcs]]
  (reduce (fn [s vf] (vf s (if (vector? attr) attr [attr])))
          subject
          v-funcs))

(defn validate
  [subject validations]
  (reduce validate-attr subject validations))

(defn valid?
  [resource]
  (empty? (:errors resource)))

(filter (fn [[_ v]] (:validation (meta v)))
        (ns-publics 'mississippi.core))

(defn generate-with-message-for [[s v]]
  (let [n (symbol (str s "-with"))]
    `(defn ~n [message#]
       (fn [subject# attr#]
         (~s subject# attr# message#)))))

(defmacro generate-with-messages []
  `(do ~@(map generate-with-message-for
              (filter (fn [[_ v]] (:validation (meta v)))
                      (ns-publics 'mississippi.core)))))

(generate-with-messages)
