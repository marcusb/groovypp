package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

@Typed
public class Issue136Test extends GroovyShellTestCase {
    void testMe () {
        shouldNotCompile """
        @Typed package p
        Map map = ['a': 1, 'b': 2]
        map.keySet().each {Date d -> }
        """
    }
}