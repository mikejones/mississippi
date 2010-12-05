(ns mississippi.test.core
  (:use [mississippi.core] :reload)
  (:use [clojure.test]))

(deftest required-attrs
  (let [r (validate { :a nil }
                    { :a [required] })]
    (is (false? (valid? r )))
    (is (=  {:a ["required"]} 
            (:errors r)))))

(deftest numeric-attrs
  (let [r (validate { :a "not a number" }
                    { :a [numeric]})]
    (is (false? (valid? r)))
    (is (= {:a ["non numeric"]}
           (:errors r)))))

(deftest member-attrs
  (let [r (validate { :a "d" }
                    { :a [(member-of #{"a" "b" "c"})]})]
    (is (false? (valid? r)))
    (is (= {:a ["is not a member of a, b, c"]}
           (:errors r)))))

(testing "attributes in a range" 
  (deftest outside-of-range
    (let [r (validate { :a 5 }
                      { :a [(in-range (range 2 4))]})]
      (is (false? (valid? r)))
      (is (= {:a ["is not a member of 2, 3"]}
             (:errors r)))))
  (deftest non-numeric
    (let [r (validate { :a "fail" }
                      { :a [(in-range (range 2 4))]})]
      (is (false? (valid? r)))
      (is (= {:a ["non numeric", "is not a member of 2, 3"]}
             (:errors r))))))

(deftest multiple-validations
  (let [r (validate {:a nil}
                    {:a [required (member-of #{"a" "b" })] })]
    (is (= {:a ["required" "is not a member of a, b"]}
           (:errors r)))))

(deftest multiple-nested-validations
  (let [o { :a { :b { :c nil :d "1" :e nil}}}
        r (validate o {[:a :b :c] [required]
                       [:a :b :d] [required numeric]
                       [:a :b :e] [required]})]
    (is (false? (valid? r)))
    (is (= {:a {:b {:c ["required"] :e ["required"]}}}
           (:errors r)))))

(deftest validating-nested-attributes
  (let [o { :a { :b { :c nil}}}
        r (validate o {[:a :b :c] [required]})]
    (is (false? (valid? r)))))
