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
           (errors {:a ["error message"]}
                   {:a [(constantly false) :msg "error message"]})))))

(testing "requied validation builder"
  (let [[validation-fn & {msg :msg when-fn :when}] (required)]
    (deftest required-validation
      (is (true?  (validation-fn "")))
      (is (true?  (validation-fn 1)))
      (is (false? (validation-fn nil))))
    
    (deftest requried-defaults
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
      (is (true?  (validation-fn [:a :b]))))
    
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

;; (testing "not blank validation"
;;   (deftest invalid-when-empty-string
;;     (is (valid? (validate {:a ""} {:a [(blank)]})))
;;     (is (not (valid? (validate {:a "test"} {:a [(blank)]}))))))

;; (testing "numeric validation"
;;   (deftest add-error-for-non-number-tpypes
;;     (let [r (validate {:a "not a number"}
;;                       {:a [(numeric)]})]
;;       (is (false? (valid? r)))
;;       (is (= ["not a number"]
;;              (get-in r [:errors :a] )))))
;;   (deftest valid-types
;;     (is (valid? (validate {:a 9}
;;                           {:a [(numeric)]})))
;;     (is (valid? (validate {:a 9.0}
;;                           {:a [(numeric)]}))))

;;   (deftest error-message-is-customisable
;;     (let [r (validate {:a nil}
;;                       {:a [(numeric :message-fn (constantly "custom message"))]})]
;;       (is (= ["custom message"]
;;              (get-in r [:errors :a]))))))



;; (testing "member of validation"
;;   (deftest is-not-valid-when-value-not-in-list
;;     (is (not (valid? (validate {:a "d"}
;;                                {:a [(member-of ["a" "b"])]})))))

;;   (deftest is-valid-when-value-is-in-list
;;     (is (valid? (validate {:a "a"}
;;                           {:a [(member-of ["a" "b"])]}))))

;;   (deftest default-message-should-list-valid-values
;;     (let [r (validate {:a "a"}
;;                       {:a [(member-of {:lat ["a" "b"]})]})]
;;       (is "is not a member of a or b"
;;           (get-in r [:errors :a]))))

;;   (deftest error-message-is-cumtomisable
;;     (let [r (validate {:a nil}
;;                       {:a [(member-of ["a"]
;;                                       :message-fn (constantly "custom message"))]})]
;;       (is (= ["custom message"] (-> r :errors :a))))))


;; (deftest multiple-validations
;;   (let [r (validate {:a nil}
;;                     {:a [(required) (numeric)] })]
;;     (is (= {:a ["required" "not a number"]}
;;            (:errors r)))))

;; (deftest multiple-nested-validations
;;   (let [o { :a { :b { :c nil :d 1 :e nil}}}
;;         r (validate o {:a {:b {:c [(required)]
;;                                :d [(required) (numeric)]
;;                                :e [(required)]}}})]
;;     (is (false? (valid? r)))
;;     (is (= {:a {:b {:c ["required"] :e ["required"]}}}
;;            (:errors r)))))

;; (testing "nested attributes"
;;   (deftest are-not-valid
;;     (let [o { :a { :b { :c nil}}}
;;           r (validate o {[:a :b :c] [(required)]})]
;;       (is (not (valid? r)))))

;;   (deftest are-valid
;;     (let [o { :a { :b { :c "foo"}}}
;;           r (validate o {[:a :b :c] [(required)]})]
;;       (is (valid? r)))))

;; (testing "functions have access to the subject and attr"
;;   (let [has-b-key?  (fn [subject] (and (some #{:b} (keys subject))
;;                                 (= [:a] *attr*)))
;;         validations {:a [(required :when-fn has-b-key?)]}]
;;     (deftest runs-validations
;;       (is (valid? (validate {:a nil} validations)))
;;       (is (not (valid? (validate {:a nil :b ""} validations)))))))
  