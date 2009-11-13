package org.mbte.groovypp.compiler

public class ProductTest extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
    @Typed
    void testProduct () {
        (0..<10).iterator().product{
            println (-10 + it + 10) // to make sure it is integer
            (0..it).iterator ()
        }.each {
            Pair<Pair,Pair> p = [it, it]
            println " \${p.first.first} \${p.second.second}"
            println " \${it.first} \${it.second} \${it.first + it.second}"
        }
    }
    testProduct ()
        """
    }
}