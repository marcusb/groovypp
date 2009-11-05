package org.mbte.groovypp.compiler

public class IterationsTest extends GroovyShellTestCase {
    void testSimple () {
        def res = shell.evaluate("""
            @Typed
            u () {
                [0,1,2,3,4,5].findAll { int it ->
                   it % 2 == 1
                }
            }
            u ()
        """)
        assertEquals ([1,3,5], res)
    }

    void testFoldLeft () {
        def res = shell.evaluate("""
            @Typed
            u () {
                ((List<Integer>)[0,1,2,3,4,5]).foldLeft(0) { el, int sum ->
                   el + sum
                }
            }
            u ()
        """)
        assertEquals (15, res)
    }

    void testFunctions () {
        def res = shell.evaluate("""
@Trait
abstract class Function0<R> {
    abstract def apply ()

    public <R1> Function0<R1> addThen (Function1<R,R1> g) {
        { -> g.apply(apply()) }
    }
}

@Trait
abstract class Function1<T,R> {
    abstract def apply (T param)

    public <R1> Function1<T,R1> addThen (Function1<R,R1> g) {
        { arg -> g.apply(apply(arg)) }
    }

    public <T1> Function1<T1,R> composeWith (Function1<T1,T> g) {
        { arg -> apply(g.apply(arg)) }
    }

    R getAt (T arg) {
        apply(arg)
    }
}

@Trait
abstract class Function2<T1,T2,R> {
    abstract def apply (T1 param1, T2 param2)

    public <R1> Function2<T1,T2,R1> addThen (Function1<R,R1> g) {
        { arg1, arg2 -> g.apply(apply(arg1, arg2)) }
    }

    Function1<T2,R> curry (T1 arg1) {
        { arg2 -> apply(arg1, arg2) }
    }

    Function1<T2,R> getAt (T1 arg1) {
        curry arg1
    }
}

@Typed u (Function2<Integer,Integer,Integer> op) {
    op.curry(10)[5].toString ()
}

@Typed v () {
   u { Integer a, Integer b ->
      a + b
   }
}

v ()

""")
        assertEquals ("15", res)
    }
}