(ns mississippi.test.core
  (:require [mississippi.core :as c]
            [clojure.test :refer [deftest is]] :reload-all))

(deftest adds-error-message-when-validation-check-fails
  (is (= {:a ["error message"]}
         (c/errors {:a nil}
                   {:a [[(constantly false) :msg "error message"]]}))))

(deftest allows-predicate-to-prevent-validation-based-on-subject-under-validation
  (letfn [(unless-has-b-key [subject] (-> subject keys #{:b}))]
    (is (= {}
           (c/errors {:a nil :b nil}
                     {:a [[(constantly false) :msg "error message" :when unless-has-b-key]]})))))

(deftest single-validations-dont-need-to-be-nested
  (is (= {:a ["error message"]}
         (c/errors {:a nil}
                   {:a [(constantly false) :msg "error message"]}))))

(deftest can-specifiy-nested-validations-as-vector-of-values
  (is (= {:a {:b ["error message"]}}
         (c/errors {:a {:b nil}}
                   {[:a :b] [(constantly false) :msg "error message"]}))))

(deftest using-validation-builders
  (is (= {:a ["required"]}
         (c/errors {:a nil} {:a (c/required)}))))

(deftest using-multiple-validation-builders
  (is (= {:a ["required" "'' is not a number"]}
         (c/errors {:a nil} {:a [(c/required) (c/numeric)]}))))

(deftest required-validation
  (is (= {} (c/errors {:a ""}
                      {:a [(c/required)]})))
  (is (= {} (c/errors {:a 1}
                      {:a [(c/required)]}))))

(deftest in-range-validation
  (is (= {:a ["'0' does not fall between 1 and 9"]}
         (c/errors {:a 0}
                   {:a [(c/in-range 1 10)]})))
  (is (= {:a ["'10' does not fall between 1 and 9"]}
         (c/errors {:a 10}
                   {:a [(c/in-range 1 10)]})))
  (is (= {}
         (c/errors {:a 1}
                   {:a [(c/in-range 1 10)]})))
  (is (= {}
         (c/errors {:a 9}
                   {:a [(c/in-range 1 10)]}))))

(deftest member-of-validation
  (is (= {:a ["'0' is not a member of :a or :b"]}
         (c/errors {:a 0}
                   {:a [(c/member-of #{:a :b})]})))
  (is (= {:a ["':c' is not a member of :a or :b"]}
         (c/errors {:a :c}
                   {:a [(c/member-of #{:a :b})]})))
  (is (= {}
         (c/errors {:a :a}
                   {:a [(c/member-of #{:a :b})]}))))

(deftest subset-of-validation
  (is (= {:a ["'[:a :c]' is not a subset of :a or :b"]}
         (c/errors {:a [:a :c]}
                   {:a [(c/subset-of #{:a :b})]})))
  (is (= {:a ["'[:c]' is not a subset of :a or :b"]}
         (c/errors {:a [:c]}
                   {:a [(c/subset-of #{:a :b})]})))
  (is (= {}
         (c/errors {:a [:a :b]}
                   {:a [(c/subset-of #{:a :b})]})))
  (is (= {}
         (c/errors {:a [:a]}
                   {:a [(c/subset-of #{:a :b})]}))))

(deftest matches-validation
  (is (= {:a ["'something1' does not match pattern of '(?i)\\b[A-Z]+\\b'"]}
         (c/errors {:a "something1"}
                   {:a [(c/matches #"(?i)\b[A-Z]+\b")]})))
  (is (= {}
         (c/errors {:a "something"}
                   {:a [(c/matches #"(?i)\b[A-Z]+\b")]})))

  (is (= {:a ["'foo.bar.baz' does not match pattern of 'foo\\.bar'"]}
         (c/errors {:a "foo.bar.baz"}
                   {:a (c/matches #"foo\.bar" :match-fn re-matches)})))
  (is (= {}
         (c/errors {:a "foo.bar"}
                   {:a (c/matches #"foo\.bar" :match-fn re-matches)}))))

(deftest matches-email-validation
  (is (= {:a ["'not-an-email' is not a valid email address"]}
         (c/errors {:a "not-an-email"}
                   {:a (c/matches-email)})))
  (is (= {:a ["'an-email@foo.com;with bobby tables' is not a valid email address"]}
         (c/errors {:a "an-email@foo.com;with bobby tables"}
                   {:a (c/matches-email)})))
  (is (= {}
         (c/errors {:a "an-email@example.com"}
                   {:a (c/matches-email)}))))

(deftest numeric-validation
  (is (= {:a ["'' is not a number"]}
         (c/errors {:a ""}
                   {:a (c/numeric)})))
  (is (= {:a ["':a' is not a number"]}
         (c/errors {:a :a}
                   {:a (c/numeric)})))
  (is (= {}
         (c/errors {:a 1}
                   {:a (c/numeric)}))))

(deftest test-non-empty-list
  (is (= {:a ["'' must be a list containing at least one item"]}
         (c/errors {:a nil}
                   {:a (c/non-empty-list)})))
  (is (= {:a ["'[]' must be a list containing at least one item"]}
         (c/errors {:a []}
                   {:a (c/non-empty-list)})))
  (is (= {:a ["'' must be a list containing at least one item"]}
         (c/errors {:a nil}
                   {:a (c/non-empty-list)})))
  (is (= {}
         (c/errors {:a [1]}
                   {:a (c/non-empty-list)}))))

(deftest msg-can-be-function
  (is (= {:attr ["string of 'bah' is too short!"]}
         (c/errors {:attr "bah"}
                   {:attr [(fn [s] (> (count s) 5))
                           :msg (fn [s] (str "string of '" s "' is too short!"))]}))))

(deftest msg-can-be-constantly-function
  (is (= {:attr ["nope"]}
         (c/errors {:attr "bah"}
                   {:attr [(fn [s] (> (count s) 5))
                           :msg (constantly "nope")]}))))

(deftest validation-function-and-msg-function-can-receive-subject-argument
  (is (= {:attr [":attr must be equal to 'bah' and subject should contain :other attribute. :attr: 'bah', subject: {:attr \"bah\"}"]}
         (c/errors {:attr "bah"}
                   {:attr [(fn [v subject]
                             (and (= "bah" v)
                                  (contains? subject :other)))
                           :msg (fn [v subject]
                                  (str ":attr must be equal to 'bah' and subject should contain :other attribute. :attr: '" v "', subject: " subject))]}))))

(deftest msg-function-can-take-subject-as-well-as-value
  (let [validation [(fn [s] (> (count s) 5))
                    :msg (fn [v s] (str "string of '" v "' is too short - and :other is '" (:other s) "'"))]
        result     (c/validate {:attr "bah"
                                :other "fooooooo"}
                               {:attr validation})]
    (is (= "string of 'bah' is too short - and :other is 'fooooooo'"
           (-> result :errors :attr first)))))

(deftest validation-function-can-receive-subject-and-value
  (is (= {:attr ["whoops"]}
         (c/errors  {:attr :x
                     :other :not-y}
                    {:attr [(fn [v s] (and (= :x v)
                                           (= :y (:other s))))
                            :msg "whoops"]}))))

(deftest should-apply-validations-to-list-of-objects
  (is (= {:a [{0 {:x ["invalid"]}
               2 {:x ["invalid"]}
               3 {:y ["invalid"]}}]}
         (c/errors {:a [{:x 1 :y 2}
                        {:x 11 :y 10}
                        {:x 10 :y 2}
                        {:x 11 :y nil}]}
                   {:a (c/list-of-objects {:x (c/in-range 11 20 :msg "invalid")
                                           :y (c/numeric :msg "invalid")})}))))

(deftest should-apply-validations-to-nested-list-of-objects
  (is (= {:a [{0 {:x [{0 {:x1 ["invalid"]}}]}
               2 {:x [{1 {:x1 ["invalid"]}}]}
               4 {:x ["required"]}
               5 {:x ["'a string?' is not a list of objects"]}}]}

         (c/errors {:a [{:x [{:x1 -10}]}
                        {:x [{:x1 1}]}
                        {:x [{:x1 5} {:x1 -1}]}
                        {:x [{:x1 11}]}
                        {}
                        {:x "a string?"}
                        {:x '()}]}

                   {:a (c/list-of-objects {:x [(c/required)
                                               (c/list-of-objects {:x1 [(fn [x] (> x 0)) :msg "invalid"]} :when :x)]})}))))