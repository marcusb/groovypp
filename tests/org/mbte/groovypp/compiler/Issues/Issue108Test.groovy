package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

@Typed
public class Issue108Test extends GroovyShellTestCase {
    void testMe () {
       shouldNotCompile """
         @Typed package test

         class Geo
         {
            String whatever

         }

         def x = { Geo a ->
           println a.whatever
         }

         x(20)
       """
    }
}