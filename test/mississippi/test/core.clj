(ns mississippi.test.core
  (:use [mississippi.core] :reload)
  (:use [clojure.test]))

(testing "required validation"
  (deftest checks-for-required-attributes
    (let [errors (errors {:a nil} {:a [(required)]})]
      (is (not (empty? (errors {:a nil} {:a [(required)]}))))
      (is (=  errors {:a ["required"]}))))

  (deftest is-valid-when-the-attribute-is-present
    (is (valid? (validate {:a :a}
                          {:a [(required)]}))))

  (deftest error-message-is-customisable
    (let [r (validate {:a nil}
                      {:a [(required :message-fn (constantly "custom message"))]})]
      (is (= ["custom message"]
             (get-in r [:errors :a])))))
  (deftest not-run-with-predicate-is-false
    (let [r (validate {:a nil}
                      {:a [(required :message-fn (constantly "custom message"))]})]
      (is (= ["custom message"]
             (get-in r [:errors :a]))))))

(testing "not blank validation"
  (deftest invalid-when-empty-string
    (let [r (validate {:a ""} {:a [not-blank]})]
      (is (false? (valid? r)))
      (is (= '("blank")
             (get-in r [:errors :a]))))
    (is (valid? (validate {:a "test"} {:a [not-blank]})))
    (is (= '("custom message")
           (get-in (validate {:a ""}
                             {:a [(not-blank :message-fn (constantly "custom message"))]})
                   [:errors :a])))))

(deftest blank-validation
  (is  (= {}
          (errors {:a ""} {:a [(blank)]})))
  (is  (= {:a '("not blank")}
          (errors {:a "value"} {:a [(blank)]})))
  (is  (= {:a '("custom message")}
          (errors {:a "value"}
                  {:a [(blank :message-fn (constantly "custom message"))]})))
  (is  (= {}
          (errors {:a "value"}
                  {:a [(blank :when-fn (constantly false))]}))))

(testing "not blank validation"
  (deftest invalid-when-empty-string
    (is (valid? (validate {:a ""} {:a [(blank)]})))
    (is (not (valid? (validate {:a "test"} {:a [(blank)]}))))))

(deftest sequence-only-contains-values-in-set
  (is (false? (valid? (validate {:a [:a :b :c]}
                                {:a [(subset-of #{:a :b})]}))))
  (is (valid? (validate {:a [:a :b]}
                        {:a [(subset-of #{:a :b :c})]}))))

(testing "numeric validation"
  (deftest add-error-for-non-number-tpypes
    (let [r (validate {:a "not a number"}
                      {:a [(numeric)]})]
      (is (false? (valid? r)))
      (is (= ["not a number"]
             (get-in r [:errors :a] )))))
  (deftest valid-types
    (is (valid? (validate {:a 9}
                          {:a [(numeric)]})))
    (is (valid? (validate {:a 9.0}
                          {:a [(numeric)]}))))

  (deftest error-message-is-customisable
    (let [r (validate {:a nil}
                      {:a [(numeric :message-fn (constantly "custom message"))]})]
      (is (= ["custom message"]
             (get-in r [:errors :a]))))))

(testing "matches regular expression validation"
  (deftest adds-error-when-attribut-does-not-match-regex
    (is (false? (valid? (validate {:a "something1"}
                                  {:a [(matches #"(?i)\b[A-Z]+\b")]}))))
    (is (valid? (validate {:a "something"}
                          {:a [(matches #"(?i)[A-Z]+")]})))
    (deftest error-message-is-cumtomisable
      (let [r (validate {:a nil}
                        {:a [(matches #"(?i)[A-Z]+") :message-fn (constantly "custom message")]})]
        (is (= '["custom message"]
               (get-in r [:errors :a])))))))

(testing "email validation"
  (deftest adds-error-when-attribut-does-not-match-regex
    (is (false? (valid? (validate {:a "not-an-email-address"}
                                  {:a [(matches-email)]}))))
    (is (valid? (validate {:a "mail@michaeljon.es"}
                          {:a [(matches-email)]}))))

  (deftest error-message-is-cumtomisable
    (let [r (validate {:a nil}
                      {:a [(matches-email :message-fn (constantly "custom message"))]})]
      (is (= '["custom message"]
             (get-in r [:errors :a]))))))

(testing "member of validation"
  (deftest is-not-valid-when-value-not-in-list
    (is (not (valid? (validate {:a "d"}
                               {:a [(member-of ["a" "b"])]})))))

  (deftest is-valid-when-value-is-in-list
    (is (valid? (validate {:a "a"}
                          {:a [(member-of ["a" "b"])]}))))

  (deftest default-message-should-list-valid-values
    (let [r (validate {:a "a"}
                      {:a [(member-of {:lat ["a" "b"]})]})]
      (is "is not a member of a or b"
          (get-in r [:errors :a]))))

  (deftest error-message-is-cumtomisable
    (let [r (validate {:a nil}
                      {:a [(member-of ["a"]
                                      :message-fn (constantly "custom message"))]})]
      (is (= ["custom message"] (-> r :errors :a))))))

(testing "attributes in a range"
  (deftest outside-of-range
    (let [r (validate { :a 11 }
                      { :a [(in-range 1 10)]})]
      (is (false? (valid? r)))
      (is (= '("does not fall between 1 and 9")
             (get-in r [:errors :a])))))

  (deftest non-numeric
    (let [r (validate {:a "fail" }
                      {:a [(in-range 1 10)]})]
      (is (false? (valid? r)))
      (is (= ["does not fall between 1 and 9"]
             (get-in r [:errors :a])))))

  (deftest error-message-is-cumtomisable
    (let [r (validate {:a 12}
                      {:a [(in-range 1 10
                                     :message-fn (constantly "custom message"))]})]
      (is (= '("custom message")
             (get-in r [:errors :a]))))))

(deftest multiple-validations
  (let [r (validate {:a nil}
                    {:a [(required) (numeric)] })]
    (is (= {:a ["required" "not a number"]}
           (:errors r)))))

(deftest multiple-nested-validations
  (let [o { :a { :b { :c nil :d 1 :e nil}}}
        r (validate o {:a {:b {:c [(required)]
                               :d [(required) (numeric)]
                               :e [(required)]}}})]
    (is (false? (valid? r)))
    (is (= {:a {:b {:c ["required"] :e ["required"]}}}
           (:errors r)))))

(testing "nested attributes"
  (deftest are-not-valid
    (let [o { :a { :b { :c nil}}}
          r (validate o {[:a :b :c] [(required)]})]
      (is (not (valid? r)))))

  (deftest are-valid
    (let [o { :a { :b { :c "foo"}}}
          r (validate o {[:a :b :c] [(required)]})]
      (is (valid? r)))))

(testing "functions have access to the subject and attr"
  (let [has-b-key?  (fn [subject] (and (some #{:b} (keys subject))
                                (= [:a] *attr*)))
        validations {:a [(required :when-fn has-b-key?)]}]
    (deftest runs-validations
      (is (valid? (validate {:a nil} validations)))
      (is (not (valid? (validate {:a nil :b ""} validations)))))))
  