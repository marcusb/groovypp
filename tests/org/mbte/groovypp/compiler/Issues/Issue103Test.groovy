package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

@Typed
public class Issue103Test extends GroovyShellTestCase {
    void testMe () {
       shouldNotCompile """
         @Typed
         class Test {
           static main(args) {
             println [].x
           }
         }
       """
    }
}