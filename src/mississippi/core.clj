(ns mississippi.core
  (:require [clojure.walk :as walk]))

(defn add-error [subject attr error]
  (assoc-in subject [:errors attr]
            (conj (get-in subject [:errors attr] []) error)))

(defn safe-parse-int [s]
  (try
    (Integer. s)
    (catch NumberFormatException e
      nil)))

(defn required [subject attr]
  (if (attr subject)
    subject
    (add-error subject attr "required")))

(defn numeric [subject attr]
  (if-let [val (safe-parse-int (attr subject))]
    (assoc subject attr val)
    (add-error subject attr "non numeric")))

(defn member-of
  [lat]
  (fn [subject attr]
    (if (contains? lat (attr subject))
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
  (reduce (fn [acc [attr v-funcs]]
            (reduce (fn [acc2 v-func] (v-func acc2 attr))
                    acc
                    v-funcs))
          subject validations))

(defprotocol Validatable
  (errors [_])
  (valid? [_]))

(defmacro defresource [name & [fields-and-validations]]
  `(defrecord ~name [~'fields]
     Validatable
     (errors [_]
             (let [v# (validate ~'fields ~fields-and-validations)]
               (if (v# :errors)
                 v#)))
     (valid? [~'this]
             (empty? (errors ~'this)))))



