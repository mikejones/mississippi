# mississippi

Mississippi provides simple validations for maps.

## Usage

Validations are provided as functions and described in a map according
to the structure of your subject.

    user> (def validations {:a [required not-blank]})
    user> (valid? (validate {:b "b"} validations))
    false
    user> (valid? (validate {:a "valid"} validations))
    true

Errors are assoc'ed onto the subject and contain further messages.

    user> (validate {:b "b"} validations)
    {:errors {:a ("required" "blank")}}

### Nested attributes

Nested attributes inside the subject map can be validated by either
building your validation map in the same structure as your subject:

    user> (def subj {:a {:b {:c "foo"}}})
    user> (def validations {:a {:b {:c [required]}}})
    user> (valid? (validate subj validations))
    true

Or by using a vector of keys:

    user> (def subj {:a {:b {:c "foo"}}})
    user> (def validations {[:a :b :c] [required]})
    user> (valid? (validate subj validations))
    true

### Custom Error Messages

Custom error messages are defined by wrapping your validation function
with a call to the `with-msg` function:

    user> (def validations {:a [(with-msg required "must be set") not-blank]})
    #'user/validations
    user> (validate {:b "b"} validations)
    {:errors {:a ("must be set" "blank")}}

You can also provide a function with you want to include the value
being validated in the error message, for example:

    (with-message valid-email #(str "'" % "' is not a valid email address"))

## Installation

Mississippi is hosted on [Clojars](http://www.clojars.org).

### Leiningen

Add the following to `:dependencies` in your `project.clj`

    [mississippi "0.1.0"]

## License

Copyright (C) 2010 Michael Jones, Gareth Jones

Distributed under the Eclipse Public License, the same as Clojure.

