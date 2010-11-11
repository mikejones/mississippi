(ns mississippi.test.core
  (:use [mississippi.core] :reload)
  (:use [clojure.test]))
  
(deftest required-attrs
  (is (= { :a nil :errors {:a ["required"]} }
         (validate { :a nil } { :a required }))))

(deftest numeric-attrs
  (is (= {:a "not a number" :errors {:a ["non numeric"]}}
         (validate { :a "not a number"} {:a numeric}))))

(deftest member-attrs
  (is (= {:a "value not in list" :errors {:a ["is not a member of a, b, c"]}}
         (validate { :a "value not in list"} {:a (member-of #{"a" "b" "c"}) }))))

(testing "attributes in a range" 
  (deftest outside-of-range
    (is (= {:a 1 :errors {:a ["is not a member of 2, 3"]}}
           (validate {:a "1"} {:a (in-range (range 2 4))}))))
  (deftest non-numeric
    (is (= {:a "a" :errors {:a ["non numeric", "is not a member of 2, 3"]}}
           (validate {:a "a"} {:a (in-range (range 2 4))})))))

(deftest multiple-validations
  (is (= {:a nil :errors {:a ["required" "is not a member of a, b"]}}
         (validate {:a nil} {:a [required (member-of #{"a" "b"})]}))))
     
