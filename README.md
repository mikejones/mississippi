# mississippi

<img src="https://github.com/downloads/mikejones/mississippi/small-logo.png"
 alt="Mississippi Logo" title="The train to Mississippi" />

Mississippi provides validation for maps.

## Usage

A simple example:

```clojure
user> (use 'mississippi.core)
user> (def subject {:a nil :b 1})
user> (def validations {:a [(comp not nil? v) :msg "required"]
                        :b [number? :msg "not numeric"]})
user> (validate subject validations)
{:a nil, :b 1, :errors {:a ("required")}}
```

Validations are defined as a map matching the structure of the map or
_subject_ to be validated. Each key in the validation map has one or
more validations defined.

## Defining Validations

A validation is a vector containing:

- a predicate function determining if the attribute is valid or not (required).
  The function takes either a single argument of the value of the attribute, or
  both the value of the attribute, and the whole subject under test.
- a key value pair of :msg => function / string
  If a function it has the same parameters as the validation function described
  above.
- a key value pair of :when => function

For example:

```clojure
{:foo [numeric? :msg "non-numeric!"]}
```

Will call the built-in `numeric?` function with the value of `:foo` in
the _subject_ being validated. If this returns false, then the `:msg`
will be assoc'd into the subject inside an `:errors` map:

```clojure
{:foo "not number" :errors {:foo ("non-numeric!")}}
```

The error value is a list, because there can be multiple validations.

### Multiple validations

Provide a vector of validation vectors to define multiple validations
for a given attribute:

```clojure
{:foo [[(comp not nil?) :msg "required"]
       [numeric?        :msg (fn [v _] (str "'" v "' is non-numeric!"))]]}
```

Would produce:

```clojure
{:foo nil :errors {:foo ("required" "'' is non-numeric!")}}
```

when applied to:

```clojure
{:foo nil}
```

## Conditional Validations

It is sometimes useful to be able to validate a given attribute based
on another inter-related attribute. This is achieved through the
`:when` option, for example:

```clojure
user> (def subject {:a 501 :b :low})
user> (def validations {:a [(fn [v] (< v 500)) :msg "too high!" :when #(= :low (:b %))]})
user> (validate subject validations)
{:a 501, :b :low, :errors {:a ("too high!")}}
```

The `:when` function takes a single argument: the subject under
validation.

## Built-in Validators

Several common-case validators are built-in for your convenience! All
are functions which return a validation vector and support the `:when`
and `:msg` options, they do however provide sensible default messages.
Validation functions that take arguments are shown with an example.

```clojure
numeric
required
member-of       ;; (member-of #{:a :b :c})
in-range        ;; (in-range 1 10)
subset-of       ;; (subset-of #{:a :b :c})
matches         ;; (matches #"foo")
matches-email
list-of-objects ;; (list-of-objects {:a [(required)]
                ;;                   :b [(numeric)]})
```

An example usage:

```clojure
user> (def validations {:a [(required)
                            (numeric)
                            (in-range 1 10)]
                        :b [(list-of-objects {:x [(required) (numeric)] :y [(required) (numeric)]})]})
user> (validate {:a nil :b [{:x 10 :y nil}]} validations)
{:a nil,
 :b [{:x 10}],
 :errors
 {:a ("required" "'' is not a number" "'' does not fall between 1 and 9"),
  :b ({0 {:y ("required" "'' is not a number")}})}}
```

The `matches` validator takes an extra optional argument of
`:match-fn`. This defaults to re-find, but you can override to use
others such as `re-seq` or `re-matches` if desired.

## Installation

Mississippi is hosted on [Clojars](http://www.clojars.org).

### Leiningen

Add the following to `:dependencies` in your `project.clj`

```clojure
[mississippi "1.0.2"]
```

## License

Copyright (C) 2010 Michael Jones, Gareth Jones

Distributed under the Eclipse Public License, the same as Clojure.
