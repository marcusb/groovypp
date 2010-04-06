package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

@Typed
public class Issue220Test extends GroovyShellTestCase {
    void testMe () {
        shouldNotCompile """
@Typed package p

class Test extends GroovyTestCase {
    void testLooping() {
        for(myVal in evaluate()) {
            println myVal
        }
    }

    def evaluate() {}
}
        """
    }
}