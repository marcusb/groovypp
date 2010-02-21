package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue107Test extends GroovyShellTestCase {
    void testMe () {
        def msg = shouldFail {
            evaluate """
    @Typed
    package test

    int sum(...someInts) {
        def total = 0
        for (int i = 0; i < someInts.size(); i++)
            total += someInts[i]
        return total
    }
    sum(1, 2)
            """
        }
        println msg
        assertTrue msg.contains("Cannot find method int.plus(Object)") 
    }
}