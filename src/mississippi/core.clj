(ns mississippi.core
  (:require [clojure.walk :as walk]))

(defn required [subject attr]
  (if-not (get-in subject attr)
    "required"))

(defn numeric [subject attr]
  (if-not (instance? Number (get-in subject attr))
    "non numeric"))

(defn member-of
  [lat]
  (fn [subject attr]
    (if-not (contains? lat
                       (get-in subject attr))
      (format "is not a member of %s"
              (apply str
                     (interpose ", " lat))))))

(defn in-range
  [r]
  (fn [subject attr]
    (map #(% subject attr)
         [numeric (member-of (set r))])))

(defn attr-errors
  [subject attr v-funcs]
  (remove nil?
          (flatten (map #(% subject attr)
                        v-funcs))))

(defn errors
  [subject validations]
  (reduce (fn [errors [attr v-funcs]]
            (let [attr-errors (attr-errors subject attr v-funcs)]
              (if(empty? attr-errors)
                errors
                (assoc-in errors attr attr-errors))))
          {}
          validations))

(defn flatten-keys* [a ks m]
  (if (map? m)
    (reduce into
            (map (fn [[k v]]
                   (flatten-keys* a (conj ks k) v))
                 (seq m)))
    (assoc a ks m)))

(defn flatten-keys
  [m]
  (flatten-keys* {} [] m))

(defn validate
  [subject validations]
  (assoc subject :errors
         (errors subject (flatten-keys validations))))

(defn valid?
  [resource]
  (empty? (:errors resource)))

