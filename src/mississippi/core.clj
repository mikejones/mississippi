(ns mississippi.core
  (:require [clojure.set :refer [subset?]]
            [clojure.string :as str]))

(declare valid? validate)

(defn- to-sentence
  [lat]
  (if (< (count lat) 2)
    (str/join ", " lat)
    (str (str/join ", " (butlast lat)) " or " (last lat))))

(defn numeric
  "Validates given value is an instance of Number."
  [& {msg :msg when-fn :when}]
  [(fn [v _] (instance? Number v))
   :msg (or msg (fn [v _] (str "'" v "' is not a number")))
   :when when-fn])

(defn required
  [& {msg :msg when-fn :when}]
  [(fn [v _] (not (nil? v)))
   :msg (or msg "required")
   :when when-fn])

(defn member-of
  "Validates the value v is contained in s (will be coerced to a set)."
  [s & {msg :msg when-fn :when}]
  [(fn [v _] (contains? (set s) v))
   :msg (or msg (fn [v _] (str "'" v "' is not a member of " (to-sentence (sort s)))))
   :when when-fn])

(defn in-range
  "Validates the value v falls between the range of start and end."
  [start end & {msg :msg when-fn :when}]
  (let [range (range start end)]
    [(fn [v _] (contains? (set range) v))
     :when when-fn
     :msg  (or msg
               (fn [v _] (str "'" v "' does not fall between " (first range) " and " (last range))))]))

(defn subset-of
  "Validates the value v is a subset of s. Both v and s will be coerced to sets."
  [s & {msg :msg when-fn :when}]
  [(fn [v _] (subset? (set v) (set s)))
   :msg (or msg (fn [v _] (str "'" v "' is not a subset of " (to-sentence (sort s)))))
   :when when-fn])

(defn matches
  "Validates the String value v matches the given Regexp re. Takes an
  optional match-fn, defaulting to re-find."
  [re & {msg :msg when-fn :when match-fn :match-fn}]
  (let [match-fn (or match-fn re-find)]
    [(fn [v _] (->> v str (match-fn re) nil? not))
     :msg (or msg (fn [v _] (str "'" v "' does not match pattern of '" re "'")))
     :when when-fn]))

(def email-regex
  #"(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$")

(defn matches-email
  "Validates the String value v matches a basic email pattern."
  [& {msg :msg when-fn :when}]
  [(fn [v _] (->> v str (re-find email-regex) nil? not))
   :msg (or msg (fn [v _] (str "'" v "' is not a valid email address")))
   :when when-fn])

(defn non-empty-list
  "Validates the attribute contains a non-empty list."
  [& {msg :msg when-fn :when}]
  [(fn [v _] (and (not (nil? v))
                  (sequential? v)
                  (> (count v) 0)))
   :msg (or msg (fn [v _] (str "'" v "' must be a list containing at least one item")))
   :when when-fn])

(defn list-of-objects
  "Validates the attribute contains a list of objects that match the provided validations.
   Can be nested as required."
  [child-validations-map & {msg :msg when-fn :when}]
  [(fn [vs _]
     (and (sequential? vs)
          (every? #(valid? (validate % child-validations-map)) vs)))
   :msg (or msg
            (fn [vs _]
              (if (sequential? vs)
                (into {}
                      (map-indexed (fn [i o]
                                     (let [errors (-> o (validate child-validations-map) :errors)]
                                       (when (seq errors)
                                         [i errors]))))
                      vs)
                (str "'" vs "' is not a list of objects"))))
   :when when-fn])

;;

(defn- flatten-keys
  ([m] (flatten-keys {} [] m))
  ([a ks m]
   (if (map? m)
     (reduce into
             (map (fn [[k v]]
                    (flatten-keys a (if (vector? k) (into ks k) (conj ks k)) v))
                  (seq m)))
     (assoc a ks m))))

(defn- call-arities
  [f value subject]
  (try
    (f value subject)
    (catch clojure.lang.ArityException _
      (f value))))

(defn- apply-validation
  [value subject [validate-fn & {when-fn :when msg :msg}]]
  (let [when-fn (or when-fn (constantly true))]
    (when (and (when-fn subject)
               (not (call-arities validate-fn value subject)))
      (if (fn? msg)
        (call-arities msg value subject)
        msg))))

(defn- errors-for
  [subject attr validations]
  (let [value       (get-in subject attr)
        validations (if (every? vector? validations)
                      validations
                      [validations])]
    (into []
          (comp (map (partial apply-validation value subject))
                (remove nil?))
          validations)))

(defn errors
  [subject validations]
  (reduce (fn [res [attr attr-validations]]
            (let [attr-errors (errors-for subject attr attr-validations)]
              (if (seq attr-errors)
                (assoc-in res attr attr-errors)
                res)))
          {}
          (flatten-keys validations)))

(defn validate
  "Apply a map of validations to a Clojure map.

  Validations should be a map where the key is the attribute in the
  subject map to be validated and the value is a vector of
  validation information to apply to the value in the subject map.

  Validation data should be in the following format. The first
  element is a function accepting:
    - single argument: of the value being validated
    - two arguments: the value being validated along with the subject under test.
  The return value should be a boolean indicating if the value is valid. 
  Subsequent values should be pairs of options. Valid options are:

   :when  a predicate function, accepting the subject under test, and returning
          if the validation should be applied
   :msg   either a string or a function used to generate the message when the validation fails.
          If a function accepts either:
            - single argument: of the value being validated
            - two arguments: the value being validated along with the subject under test.

  For example, given a subject of:

  {:a \"some string\"}

  A possible validations map could be:

  {:a [(fn [v _] (not (nil? v))) :msg \"you forgot to supply a value\"] }

  A number of builders for common validations have been provided they accept
  the same options and are applied in the same way.

  In the example above the validation could be replaced by:

  {:a (required :msg \"you forgot to supply a value\")]}

  Multiple validations can be provided by supplying a vector of vectors containing
  validation data. For example for the subject:

  {:a 1}

  A possible validation map could be

  {:a [(required) (numeric :msg (fn [v _] (str \"'\" v \"' is not a number\")))]}

  The function returns a map containing the messages of the validation failures or an
  empty map. For example:

  (errors {:a nil} {:a (required)}

  Returns

  {:a [\"required\"]}

  To validate nested maps, simply mirror the subject's shape in the validations:

  (validate {:a {:b {:c \"foo\"}}}
            {:a {:b {:c [(required)]}}})

  An alternative syntax for validating nested attributes is to provide
  the key as a vector of keys:

  {[:a :b :c] [(required)]}"
  [subject validations]
  (assoc subject :errors (errors subject validations)))

(defn valid?
  "Checks if the map contains any errors"
  [resource]
  (empty? (:errors resource)))

