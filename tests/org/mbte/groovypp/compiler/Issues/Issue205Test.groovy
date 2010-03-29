package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldCompile

public class Issue205Test extends GroovyShellTestCase {
    void testExceptionPropagationOfCompilationError() {
      shouldCompile("""
                @Typed
                class CloneTest extends GroovyTestCase {
                    List numbers = [1, 2]
                    void testClone() {
                        def newNumbers = ((ArrayList)numbers).clone()
                    }
                }""")
    }
}