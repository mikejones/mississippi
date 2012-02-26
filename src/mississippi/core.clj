(ns mississippi.core
  (:use [clojure.string :only (blank?)]
        [clojure.set :only (difference subset?)]))

(defn- to-sentence
  [lat]
  (let [to-csv (fn [x] (apply str (interpose ", " x)))]
    (if (< (count lat) 2)
      (to-csv lat)
      (str (to-csv (butlast lat)) " or " (last lat)))))

;; (defn- build-valiation-fn
;;   "Build a valiation fn that accepts a value and returns nil if the validation passes
;;    or a string describing the error if it fails.

;;    :when-fn  a predicate function, accepting the subject under test, and returning
;;              if the validation should be applied 
;;    :valid-fn a function, accepting the value being tested, and returning a
;;              boolean indicating if the value was valid
;;    :msg      either a string or a function used the message when the validation fails.
;;              If a function accepts a single argument of the value being validated"
;;   [{:keys [valid-fn msg when-fn]}]
;;   (let [when-fn (or when-fn (constantly true))]
;;     (fn [v]
;;       (when (and (when-fn *subject*)
;;                  (not (valid-fn v)))
;;         (if (ifn? msg)
;;           (msg v)
;;           msg)))))

;; (defn blank
;;   "Returns a fn that validates given string value is blank."
;;   [& {:keys [message-fn when-fn]}]
;;   (build-valiation-fn {:valid-fn blank?
;;                        :mgs      (or message-fn "not blank")
;;                        :when-fn  when-fn}))

(defn numeric
  "Validates given value is an instance of Number."
  [& {msg :msg when-fn :when}]
  [(fn [v] (instance? Number v))
   :msg (or msg "not a number")
   :when when-fn])

(defn required
  [& {msg :msg when-fn :when}]
  [(comp not nil?) :msg "required" :when when-fn])

(defn member-of
  "Validates the value v is contained in s (will be coerced to a set)."
  [s & {msg :msg when-fn :when}]
  [(comp not nil? (set s))
   :msg (or msg (str "not a member of " (to-sentence s)))
   :when when-fn])

(defn in-range
  "Validates the value v falls between the range of start and end."
  [start end & {msg :msg when-fn :when}]
  (let [range (range start end)]
    [(comp not nil? (set range))
     :when when-fn
     :msg  (or msg
               (str "does not fall between " (first range) " and " (last range)))]))

(defn subset-of
  "Validates the value v is a subset of s. Both v and s will be coerced to sets."
  [s & {msg :msg when-fn :when}]
  [(fn [v] (subset? (set s) (set v)))
   :msg (or msg (str "not a subset of " (to-sentence s)))
   :when when-fn])

(defn matches
  "Validates the String value v matches the given Regexp re."
  [re & {msg :msg when-fn :when}]
  [(fn [v] (->> v str (re-find re) nil? not))
   :msg (or msg (str "does not match pattern of '" re "'"))
   :when when-fn])

(def email-regex
  #"(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}\b")

(defn matches-email
  "Validates the String value v matches a basic email pattern."
  [& {msg :msg when-fn :when}]
  [(fn [v] (->> v str (re-find email-regex) nil? not))
   :msg (or msg "invalid email address")
   :when-fn when-fn])

(defn- flatten-keys
  ([m] (flatten-keys {} [] m))
  ([a ks m]
     (if (map? m)
       (reduce into
               (map (fn [[k v]]
                      (flatten-keys a (if (vector? k) (into ks k) (conj ks k)) v))
                    (seq m)))
       (assoc a ks m))))

(defn- apply-validation
  [value subject [validate-fn & {when-fn :when msg :msg :or {when-fn (constantly true)}}]]
  (when (and (when-fn subject)
             (not (validate-fn value)))
    msg))

(defn errors
  [subject validations]
  (reduce (fn [errors [attr attr-validations]]
            (let [value (get-in subject attr)]
              (let [attr-errors (->> (if (every? vector? attr-validations)
                                       attr-validations
                                       [attr-validations])
                                     (map (partial apply-validation value subject))
                                     flatten
                                     (remove nil?))]
                (if-not (empty? attr-errors)
                  (assoc-in errors attr attr-errors)
                  errors))))
          {}
          (flatten-keys validations)))

;; (defn validate
;;   "Apply a map of validation functions to a Clojure map.

;;   Validations should be a map where the key is the attribute in the
;;   subject map to be validated, and the value is a sequence of
;;   validation functions to apply to the value in the subject map. For
;;   example, given a subject of:

;;   {:a \"some string\"
;;    :b 12
;;    :c {:d nil}}

;;   A possible validations map could be:

;;   {:a [(required)]
;;    :b [(required) (numeric :message-fn (fn [v] (str % \" is not a number\")))]
;;    :c {:d [(required :when-fn (fn [_] (some #{:a} (keys *subject*)]}}

;;   An alternative syntax for validating nested attributes is to provide the key as a vector:

;;   {[:c :d] [(required)]}"
;;   [subject validations]
;;   (assoc subject
;;     :errors (errors subject validations)))

;; (defn valid?
;;   "Checks if the map contains any errors"
;;   [resource]
;;   (empty? (:errors resource)))

