# mississippi

Mississippi provides validation for maps.

## Usage

A simple example:

    user> (use 'mississippi.core)
    user> (def subject {:a nil :b 1})
    user> (def validations {:a [(comp not nil?) :msg "required"]
                            :b [number? :msg "not numeric"]})
    user> (validate subject validations)
    {:a nil, :b 1, :errors {:a ("required")}}

Validations are defined as a map matching the structure of the map or
_subject_ to be validated. Each key in the validation map has one or
more validations defined.

## Defining Validations

A validation is a vector containing: 

* a predicate function determining if the attribute is valid or not (required)
* a key value pair of :msg => function / string 
* a key value pair of :when => function

For example:

    {:foo [numeric? :msg "non-numeric!"]}

Will call the built-in `numeric?` function with the value of `:foo` in
the _subject_ being validated. If this returns false, then the `:msg`
will be assoc'd into the subject inside an `:errors` map:

    {:foo "not number" :errors {:foo ("non-numeric!")}}

The error value is a list, because there can be multiple validations.

### Multiple validations

Provide a vector of validation vectors to define multiple validations
for a given attribute:

    {:foo [[(comp not nil?) :msg "required"]
           [numeric?        :msg "non-numeric!"]]}

Would produce:

    {:foo nil :errors {:foo ("required" "non-numeric!")}}

when applied to:

    {:foo nil}

## Conditional Validations

It is sometimes useful to be able to validate a given attribute based
on another inter-related attribute. This is achieved through the
`:when` option, for example:

    user> (def subject {:a 501 :b :low})
    user> (def validations {:a [#(< % 500) :msg "too high!" :when #(= :low (:b %))]})
    user> (validate subject validations)
    {:a 501, :b :low, :errors {:a ("too high!")}}

The `:when` function takes a single argument: the subject under
validation.

## Built-in Validators

Several common-case validators are built-in for your convenience! All
are functions which return a validation vector and support the `:when`
and `:msg` options, they do however provide sensible default messages.
Validation functions that take arguments are shown with an example.

    numeric
    required
    member-of     ;; (member-of #{:a :b :c})
    in-range      ;; (in-range 1 10)
    subset-of     ;; (subset-of #{:a :b :c})
    matches       ;; (matches #"foo")
    matches-email

An example usage:

    user> (def validations {:a [(required)
                                (numeric)
                                (in-range 1 10)]})
    user> (validate {:a nil} validations)
    {:errors {:a ("required" "not a number" "does not fall between 1 and 9")}}

## Installation

Mississippi is hosted on [Clojars](http://www.clojars.org).

### Leiningen

Add the following to `:dependencies` in your `project.clj`

    [mississippi "1.0.0"]

## License

Copyright (C) 2010 Michael Jones, Gareth Jones

Distributed under the Eclipse Public License, the same as Clojure.

