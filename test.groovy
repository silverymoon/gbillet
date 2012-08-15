class Foo implements Runnable {

    def id
    def closure

    Foo(name, cls){
        this.id = name
        this.closure = cls
    }

    String toString(){
        return id
    }

    void run(){
        closure()
    }

    int hashCode(){
        return id.hashCode()
    }

    boolean equals(obj){
        return obj?.id == id
    }
}

def a = new Foo("foo", {println 42})
def b = new Foo("bar", {println 3.1415})
def c = new Foo("quz", {println "baz"})
def a1 = new Foo("foo", {println 42})
println a.hashCode()
println a1.hashCode()
println "a is a1? " + a.is(a1)
println "a equals a1? " + a.equals(a1)
println "a == a1? " + (a == a1)
def l = [a, b]
println "l 1: $l"
l << c
println "l 2: $l"
l -= a1
println "l 3: $l"
l << a
println "l 4: $l"
