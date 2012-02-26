(ns mississippi.test.core
  (:use [mississippi.core] :reload)
  (:use [clojure.test]))

(testing "generating errors"
  (deftest adds-error-message-when-validation-check-fails
    (is (= {:a ["error message"]}
           (errors {:a nil} {:a [[(constantly false) :msg "error message"]]}))))

  (deftest allows-predicate-to-prevent-validation-based-on-subject-under-validation
    (letfn [(unless-has-b-key [subject] (-> subject keys #{:b}))]
      (is (= {}
             (errors {:a nil :b nil}
                     {:a [[(constantly false) :msg "error message" :when unless-has-b-key]]})))))

  (deftest single-validations-dont-need-to-be-nested
    (is (= {:a ["error message"]}
           (errors {:a nil}
                   {:a [(constantly false) :msg "error message"]}))))

  (deftest can-specifiy-nested-validations-as-vector-of-values
    (is (= {:a {:b ["error message"]}}
           (errors {:a {:b nil}}
                   {[:a :b] [(constantly false) :msg "error message"]}))))
  (deftest using-validation-builders
    (is (= {:a ["required"]}
           (errors {:a nil} {:a (required)}))))

  (deftest using-multiple-validation-builders
    (is (= {:a ["required" "not a number"]}
           (errors {:a nil} {:a [(required) (numeric)]})))))

(testing "required validation builder"
  (let [[validation-fn & {msg :msg when-fn :when}] (required)]
    (deftest required-validation
      (is (true?  (validation-fn "")))
      (is (true?  (validation-fn 1)))
      (is (false? (validation-fn nil))))
    
    (deftest required-defaults
      (is (= "required" msg))
      (is (nil? when-fn)))))

(testing "in-range validation builder"
  (let [[validation-fn & {msg :msg when-fn :when}] (in-range 1 10)]
    (deftest in-range-validation
      (is (false? (validation-fn 0)))
      (is (false? (validation-fn 10)))
      (is (true?  (validation-fn 9)))
      (is (true?  (validation-fn 1))))
    
    (deftest in-range-defaults
      (is (= msg "does not fall between 1 and 9"))
      (is (nil? when-fn)))))

(testing "member-of validation builder"
  (let [[validation-fn & {msg :msg when-fn :when}] (member-of #{:a :b})]
    (deftest member-of-validation
      (is (false? (validation-fn 0)))
      (is (false? (validation-fn :c)))
      (is (true?  (validation-fn :a))))
    
    (deftest member-of-defaults
      (is (= msg "not a member of :a or :b"))
      (is (nil? when-fn)))))

(testing "subset-of validation builder"
  (let [[validation-fn & {msg :msg when-fn :when}] (subset-of #{:a :b})]
    (deftest subset-of-validation
      (is (false? (validation-fn [:a :c])))
      (is (false? (validation-fn [:c])))
      (is (true?  (validation-fn [:a :b])))
      (is (true?  (validation-fn [:a]))))
    
    (deftest subset-of-defaults
      (is (= msg "not a subset of :a or :b"))
      (is (nil? when-fn)))))

(testing "matches validation builder"
  (let [[validation-fn & {msg :msg when-fn :when}] (matches #"(?i)\b[A-Z]+\b")]
    (deftest matches-validation
      (is (false? (validation-fn "something1")))
      (is (true?  (validation-fn "something"))))
    
    (deftest matches-defaults
      (is (= msg "does not match pattern of '(?i)\\b[A-Z]+\\b'"))
      (is (nil? when-fn)))))

(testing "email validation builder"
  (let [[validation-fn & {msg :msg when-fn :when}] (matches-email)]
    (deftest matches-email-validation
      (is (false? (validation-fn "not-an-email")))
      (is (true?  (validation-fn "an-email@example.com"))))
    
    (deftest matches-defaults
      (is (= msg "invalid email address"))
      (is (nil? when-fn)))))

(testing "numeric validation builder"
  (let [[validation-fn & {msg :msg when-fn :when}] (numeric)]
    (deftest numeric-validation
      (is (false? (validation-fn "")))
      (is (false? (validation-fn :a)))
      (is (true?  (validation-fn 1))))
    
    (deftest numeric-defaults
      (is (= msg "not a number"))
      (is (nil? when-fn)))))

