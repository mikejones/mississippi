(ns mississippi.core
  (:require [clojure.walk :as walk]))

(defn required
  "Validates that the specified attr is not blank.

   The following options are avaliable:
     :message
       Override the default message"
  ([subject attr]
     ((required {}) subject attr))
  ([{:keys [message] :or {message "required"}}]
     (fn [subject attr]
       (if-not (get-in subject attr)
         message))))

(defn numeric
  "Validates that the attribute is an instance of Number

   The following options are available:
     :message
       Override the default message"
  ([subject attr]
     ((numeric {}) subject attr))
  
  ([{:keys [message] :or {message "non numeric"}}]
     (fn [subject attr]
       (if-not (instance? Number
                          (get-in subject attr))
         message))))

(defn- to-sentence
  [lat]
  (apply str
         (cond (= (count lat) 1) lat 
               (= (count lat) 2) [(first lat) " or " (second lat)]
               :else (loop [coll lat, acc []]
                       (cond (empty? coll) acc
                             (= (count coll) 2) (conj acc
                                                      (first coll) " or " (second coll))
                             :else (recur (rest coll)
                                          (conj acc
                                                (first coll) ", ")))))))

(defn member-of
  "Validates that the attribute is a member is contained in a list.

   lat - a list of valid values

   The following options are available:
     :message
       Override the default message"

  ([lat]
     (member-of lat {}))
  ([lat {:keys [message]}]
     (fn [subject attr]
       (if-not (some #{(get-in subject attr)}
                     lat)
         (or message
             (format "is not a member of %s"
                     (to-sentence lat)))))))

(defn in-range
  "Validates that an attribute is numeric and falls within a range.

   start - the start of the range
   end   - the end of the range

   The following options are available:
     :message
       Override the default message"
  ([start end]
     (in-range start end {}))
  ([start end {:keys [message]}]
     (let [r (range start end)]
       (fn [subject attr]
         (map #(% subject attr)
              [numeric
               (member-of r 
                          {:message (or message
                                        (format "does not fall between %s and %s"
                                                (first r)
                                                (last r)))})])))))

(defn matches
  "Validates that the attribute matches a specified format
   m - regular expression to match agains

   The following options are available:
     :message
       Override the default message"
  ([m]
     (matches m {}))
  ([m {:keys [message] :or {message "does to match format"}}]
     (fn [subject attr]
       (if-not (re-find m
                        (str (get-in subject attr)))
         message))))

(defn matches-email
  "Validates that the attribute matches as an email address
   (see http://www.regular-expressions.info/email.html for limitations)

   The following options are available:
     :message
       Override the default message"
  ([subject attr]
     ((matches-email {}) subject attr))
  ([{:keys [message] :or {message "invalid email address"}}]
     (matches #"(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}\b"
              {:message message})))

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

(defn- attr-errors
  [subject attr v-funcs]
  (remove nil?
          (flatten (map #(% subject attr)
                        v-funcs))))

(defn errors
  "Return the errors from applying the validations to the subject

   subject    - a map of values
   validation - a map of validations to apply"
  [subject validations]
  (reduce (fn [errors [attr v-funcs]]
            (let [attr-errors (attr-errors subject attr v-funcs)]
              (if(empty? attr-errors)
                errors
                (assoc-in errors attr attr-errors))))
          {}
          validations))

(defn validate
  "Apply validations to a map.

   subject     - a map of values to be validated
   validations - a map of validations to check the subject against"
  [subject validations]
  (assoc subject :errors
         (errors subject (flatten-keys validations))))

(defn valid?
  "Checks if the map contains any errors"
  [resource]
  (empty? (:errors resource)))

