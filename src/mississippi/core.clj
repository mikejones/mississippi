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

(defn required [subject attr]
  (if (get-in subject attr)
    subject
    (add-error subject attr "required")))

(defn numeric [subject attr]
  (if-let [val (safe-parse-int (get-in subject attr))]
    (assoc-in subject attr val)
    (add-error subject attr "non numeric")))

(defn member-of
  [lat]
  (fn [subject attr]
    (if (contains? lat (get-in subject attr))
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

