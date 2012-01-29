# mississippi

Mississippi provides simple validations for maps.

## Usage

Validations are provided as functions and described in a map according to the structure of your subject.

    user> (def validations {:a [required not-blank]})
    user> (valid? (validate {:b "b"} validations))
    false
    user> (valid? (validate {:a "valid"} validations))
    true

Errors are assoc'ed onto the subject and contain further messages.

    user> (validate {:b} validations)
    {:errors {:a ("required" "blank")}}
    
### Custom Error Messages

    user> (def validations {:a [(required {:message "must be set"}) not-blank]})
    #'user/validations
    user> (validate {:b} validations)
    {:errors {:a ("must be set" "blank")}}

## Installation

Mississippi is hosted on [Clojars](http://www.clojars.org).

### Leiningen

Add the following to `:dependencies` in your `project.clj`

    [mississippi "0.0.3-SNAPSHOT"]

## License

Copyright (C) 2010 FIXME

Distributed under the Eclipse Public License, the same as Clojure.

# TODO

* Implement nested validations: `{ :foo { :bar [validations] }}`
* Override error messages
* Validate vectors in a more generic way (maybe) 
