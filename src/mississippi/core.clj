(ns mississippi.core
  (:use [clojure.string :only (blank?)]
        [clojure.set :only (difference subset?)]))

(def ^:dynamic *subject*)
(def ^:dynamic *attr*)

(defn- to-sentence
  [lat]
  (let [to-csv (fn [x] (apply str (interpose ", " x)))]
    (if (< (count lat) 2)
      (to-csv lat)
      (str (to-csv (butlast lat)) " or " (last lat)))))

(defn- build-valiation-fn
  [{:keys [validation message-fn when-fn]}]
  (let [when-fn (or when-fn (constantly true))]
    (fn [v]
      (when (and (when-fn *subject*)
                 (not (validation v)))
        (message-fn v)))))

(defn required
  "Validates given value is not nil.
   opts
    :when-fn       only apply the validation if this function returns true
    :message-fn    a function accepting thevalue and returning a string for
                   the error message"
  [& {:keys [message-fn when-fn]}]
  (build-valiation-fn {:validation #(not (nil? %))
                       :message-fn (or message-fn (constantly "required"))
                       :when-fn    when-fn}))

(defn not-blank
  "Validates given string value is not blank."
  [& {:keys [message-fn when-fn]}]
  (build-valiation-fn {:validation #(not (blank? %))
                       :message-fn (or message-fn (constantly "blank"))
                       :when-fn    when-fn}))

(defn blank
  "Returns a fn that validates given string value is blank."
  [& {:keys [message-fn when-fn]}]
  (build-valiation-fn {:validation blank?
                       :message-fn (or message-fn (constantly "not blank"))
                       :when-fn    when-fn}))

(defn numeric
  "Validates given value is an instance of Number."
  [& {:keys [message-fn when-fn]}]
  (build-valiation-fn {:validation #(instance? Number %)
                       :message-fn (or message-fn (constantly "not a number"))
                       :when-fn    when-fn}))

(defn member-of
  "Validates the value v is contained in s (will be coerced to a set)."
  [s & {:keys [message-fn when-fn]}]
  (build-valiation-fn {:validation (fn [v] (some #{v} (set s)))
                       :message-fn (or message-fn
                                       (constantly (str "not a member of " (to-sentence s))))
                       :when-fn    when-fn}))

(defn in-range
  "Validates the value v is both numeric and falls between the range of start and end."
  [start end & {:keys [message-fn when-fn]}]
  (let [range        (range start end)]
    (build-valiation-fn {:validation (fn [v] (and (instance? Number v)
                                                 (some #{v} (set range))))
                         :message-fn (or message-fn
                                         (constantly (str "does not fall between " (first range)
                                                          " and " (last range))))
                         :when-fn    when-fn})))

(defn subset-of
  "Validates the value v is a subset of s. Both v and s will be coerced to sets."
  [s & {:keys [message-fn when-fn]}] 
  (build-valiation-fn {:validation (fn [v] (subset? (set v) (set s)))
                       :message-fn (or message-fn
                                       (constantly (str "not a subset of " (set s))))
                       :when-fn    when-fn}))

(defn matches
  "Validates the String value v matches the given Regexp re."
  [re {:keys [message-fn when-fn]}]
  (build-valiation-fn {:validation (fn [v] (re-find re (str v)))
                       :message-fn (or message-fn
                                       (constantly (str "does not match pattern of '" re "'")))
                       :when-fn    when-fn}))

(defn matches-email
  "Validates the String value v matches a basic email pattern."
  [& {:keys [message-fn when-fn]}]
  (build-valiation-fn {:validation #(re-find #"(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}\b"
                                             (str %))
                       :message-fn (or message-fn (constantly "invalid email address"))
                       :when-fn when-fn}))

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

   subject     - a map of values
   validations - a map of validations to apply"
  [subject validations]
  (binding [*subject* subject]
    (reduce (fn [errors [attr v-funcs]]
              (binding [*attr* attr]
                (let [attr-errors (attr-errors (get-in subject attr) v-funcs)]
                  (if (empty? attr-errors)
                    errors
                    (assoc-in errors attr attr-errors)))))
            {}
            (flatten-keys validations))))

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

  {:a [(required)]
   :b [(required) (numeric :message-fn (fn [v] (str % \" is not a number\")))]
   :c {:d [(required :when-fn (fn [_] (some #{:a} (keys *subject*)]}}

  An alternative syntax for validating nested attributes is to provide the key as a vector:

  {[:c :d] [(required)]}"
  [subject validations]
  (assoc subject
    :errors (errors subject validations)))

(defn valid?
  "Checks if the map contains any errors"
  [resource]
  (empty? (:errors resource)))

