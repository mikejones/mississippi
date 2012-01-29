(ns mississippi.core
  (:use [clojure.string :only (blank?)]
        [clojure.set :only (difference subset?)]))

(defn- to-sentence
  [lat]
  (let [to-csv (fn [x] (apply str (interpose ", " x)))]
    (if (< (count lat) 2)
      (to-csv lat)
      (str (to-csv (butlast lat)) " or " (last lat)))))

(defn with-msg
  "Wrap a validation function with a custom message.

   f - a validation function
   msg - either a string or a function, if a function takes a single argument of the value being validated" [f
   msg]
  (fn [v]
    (when (f v)
      (cond
       (ifn? msg) (msg v)
       :else msg))))

(defn required
  "Validates given value is not nil."
  [v]
  (when-not v "required"))

(defn not-blank
  "Validates given string value is not blank."
  [v]
  (when (blank? v) "blank"))

(defn numeric
  "Validates given value is an instance of Number."
  [v]
  (when-not (instance? Number v) "non numeric"))

(defn member-of
  "Validates the value v is contained in s (will be coerced to a set)."
  [s]
  (fn [v]
    (when-not (some #{v} (set s))
      (str "is not a member of " (to-sentence s)))))

(defn subset-of
  "Validates the value v is a subset of s. Both v and s will be coerced to sets."
  [s]
  (fn [v]
    (when-not (subset? (set v) (set s))
      (str "not a subset of " (set s)))))

(defn in-range
  "Validates the value v is both numeric and falls between the range of start and end."
  [start end]
  (let [r (range start end)]
    (fn [v]
      (let [member-fn (with-msg (member-of (set r))
                        (str "does not fall between " (first r) " and " (last r)))]
        [(numeric v)
         (member-fn v)]))))

(defn matches
  "Validates the String value v matches the given Regexp re."
  [re]
  (fn [v]
    (when-not (re-find re (str v))
      (str "does not match pattern of '" re "'"))))

(defn matches-email
  "Validates the String value v matches a basic email pattern."
  [v]
  ((with-msg (matches #"(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}\b")
     "invalid email address") v))

(defn validate-if
  "Only run the given validation functions if the condition predicate
  evaluates to true."
  [condition & validations]
  (fn [value]
    (if (condition value)
      (for [validation validations]
        (validation value)))))

(defn- flatten-keys
  ([m] (flatten-keys {} [] m))
  ([a ks m]
     (if (map? m)
       (reduce into
               (map (fn [[k v]]
                      (flatten-keys a (if (vector? k) (into ks k) (conj ks k)) v))
                    (seq m)))
       (assoc a ks m))))

(defn- attr-errors
  [value v-funcs]
  (->> (map #(%1 %2) v-funcs (repeat value))
       flatten
       (remove nil?)))

(defn errors
  "Return the errors from applying the validations to the subject

   subject    - a map of values
   validation - a map of validations to apply"
  [subject validations]
  (reduce (fn [errors [attr v-funcs]]
            (let [attr-errors (attr-errors (get-in subject attr) v-funcs)]
              (if(empty? attr-errors)
                errors
                (assoc-in errors attr attr-errors))))
          {}
          validations))

(defn validate
  "Apply a map of validation functions to a Clojure map.

  Validations should be a map where the key is the attribute in the
  subject map to be validated, and the value is a sequence of
  validation functions to apply to the value in the subject map. For
  example, given a subject of:

  {:a \"some string\"
   :b 12
   :c {:d nil}}

  A possible validations map could be:

  {:a [required]
   :b [required numeric]
   :c {:d [required]}}

  An alternative syntax for validating nested attributes is to provide the key as a vector:

  {[:c :d] [required]}"
  [subject validations]
  (assoc subject
    :errors (errors subject (flatten-keys validations))))

(defn valid?
  "Checks if the map contains any errors"
  [resource]
  (empty? (:errors resource)))

