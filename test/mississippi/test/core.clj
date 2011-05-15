(ns mississippi.test.core
  (:use [mississippi.core] :reload)
  (:use [clojure.test]))

(testing "required validation"
  (deftest checks-for-required-attributes
    (let [r (validate { :a nil }
                      { :a [required] })]
      (is (false? (valid? r )))
      (is (=  {:a ["required"]} 
              (:errors r)))))

  (deftest is-valid-when-the-attribute-is-present
    (is (valid? (validate {:a :a}
                          {:a [required]}))))
  
  (deftest error-message-is-customisable
    (let [r (validate {:a nil}
                      {:a [(required {:message "custom message"})]})]
      (is (= '("custom message") 
             (get-in r [:errors :a]))))))

(testing "numeric validation"
  (deftest add-error-for-non-number-tpypes
    (let [r (validate {:a "not a number"}
                      {:a [numeric]})]
      (is (false? (valid? r)))
      (is (= ["non numeric"]
             (get-in r [:errors :a] )))))
  (deftest valid-types
    (is (valid? (validate {:a 9}
                          {:a [numeric]})))
    (is (valid? (validate {:a 9.0}
                          {:a [numeric]}))))
  
  (deftest error-message-is-customisable
    (let [r (validate {:a nil}
                      {:a [(numeric {:message "custom message"})]})]
      (is (= '("custom message") 
             (get-in r [:errors :a]))))))

(testing "matches regular expression validation"
  (deftest adds-error-when-attribut-does-not-match-regex
    (is (false? (valid? (validate {:a "something1"}
                                  {:a [(matches #"(?i)\b[A-Z]+\b")]}))))
    (is (valid? (validate {:a "something"}
                          {:a [(matches #"(?i)[A-Z]+")]})))
    (deftest error-message-is-cumtomisable
      (let [r (validate {:a nil}
                        {:a [(matches #"(?i)[A-Z]+" {:message "custom message"})]})]
      (is (= '("custom message") 
             (get-in r [:errors :a])))))))

(testing "email validation"
  (deftest adds-error-when-attribut-does-not-match-regex
    (is (false? (valid? (validate {:a "not-an-email-address"}
                                  {:a [matches-email]}))))
    (is (valid? (validate {:a "mail@michaeljon.es"}
                          {:a [matches-email]}))))
  
  (deftest error-message-is-cumtomisable
      (let [r (validate {:a nil}
                        {:a [(matches-email {:message "custom message"})]})]
        (is (= '("custom message") 
               (get-in r [:errors :a]))))))

(testing "member of validaiton"
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
                      {:a [(member-of ["a"] { :message "custom message"})]})]
      (is (= '("custom message") 
             (get-in r [:errors :a]))))))

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
      (is (= ["non numeric", "does not fall between 1 and 9"]
               (get-in r [:errors :a])))))
  
  (deftest error-message-is-cumtomisable
    (let [r (validate {:a 12}
                      {:a [(in-range 1 10 { :message "custom message"})]})]
      (is (= '("custom message") 
             (get-in r [:errors :a]))))))

(deftest multiple-validations
  (let [r (validate {:a nil}
                    {:a [required numeric] })]
    (is (= {:a ["required" "non numeric"]}
           (:errors r)))))

(deftest multiple-nested-validations
  (let [o { :a { :b { :c nil :d 1 :e nil}}}
        r (validate o {:a {:b {:c [required]
                               :d [required numeric]
                               :e [required]}}})]
    (is (false? (valid? r)))
    (is (= {:a {:b {:c ["required"] :e ["required"]}}}
           (:errors r)))))

(deftest validating-nested-attributes
  (let [o { :a { :b { :c nil}}}
        r (validate o {[:a :b :c] [required]})]
    (is (false? (valid? r)))))
