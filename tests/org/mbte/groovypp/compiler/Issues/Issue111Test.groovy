package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

@Typed
public class Issue111Test extends GroovyShellTestCase {
    void testMe () {
       shouldNotCompile """
         @Typed package test

         Reference data = 'original'
       """
    }
}