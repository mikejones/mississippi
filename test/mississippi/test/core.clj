(ns mississippi.test.core
  (:use [mississippi.core] :reload)
  (:use [clojure.test]))

(defresource Required { :a [required] })

(deftest required-attrs
  (let [r (Required. { :a nil })]
    (is (false? (valid? r)))
    (is (= { :a nil :errors {:a ["required"]}} 
           (errors r)))))

(defresource Numeric { :a [numeric] })

(deftest numeric-attrs
  (let [r (Numeric. { :a "not a number" })]
    (is (false? (valid? r)))
    (is (= {:a "not a number" :errors {:a ["non numeric"]}}
           (errors r)))))

(defresource Member { :a [(member-of #{"a" "b" "c"})]})

(deftest member-attrs
  (let [r (Member. { :a "d" })]
    (is (false? (valid? r)))
    (is (= {:a "d" :errors {:a ["is not a member of a, b, c"]}}
           (errors r)))))

(defresource Range { :a [(in-range (range 2 4))]})

(testing "attributes in a range" 
  (deftest outside-of-range
    (let [r (Range. { :a 5 })]
      (is (false? (valid? r)))
      (is (= {:a 5 :errors {:a ["is not a member of 2, 3"]}}
             (errors r)))))
  (deftest non-numeric
    (let [r (Range. { :a "fail" })]
      (is (false? (valid? r)))
      (is (= {:a "fail" :errors {:a ["non numeric", "is not a member of 2, 3"]}}
             (errors r))))))

(defresource Multiple { :a [required (member-of #{"a" "b" })] })

(deftest multiple-validations
  (let [r (Multiple. {:a nil})]
    (is (= {:a nil :errors {:a ["required" "is not a member of a, b"]}}
           (errors r)))))
