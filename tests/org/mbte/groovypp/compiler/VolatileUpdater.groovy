package org.mbte.groovypp.compiler

public class VolatileUpdater extends GroovyShellTestCase {
    void testReference () {
        def res = shell.evaluate ("""
            @Typed class A<T> {
                volatile T field

                boolean reset (T newValue) {
                   field.compareAndSet(null, newValue)
                }
            }

            new A<Integer> ().reset (10)
        """)

        assertTrue res
    }

    void testInt () {
        def res = shell.evaluate ("""
            @Typed class A {
                volatile int field

                def reset (int newValue) {
                   field.incrementAndGet()
                }
            }

            new A ().reset (1)
        """)

        assertEquals 1, res
    }

    void testLong () {
        def res = shell.evaluate ("""
            @Typed class A<T> {
                volatile long field

                def reset () {
                   field.getAndIncrement()
                   field.getAndIncrement()
                   field.getAndIncrement()
                }
            }

            new A ().reset ()
        """)

        assertEquals 2L, res
    }
}