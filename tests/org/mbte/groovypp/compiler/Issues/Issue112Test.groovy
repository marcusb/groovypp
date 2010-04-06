package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue112Test extends GroovyShellTestCase {
    void testMe () {
       shell.evaluate """
         @Typed(TypePolicy.MIXED)
         package test

         class A {}
         class B {}

         A a = new A()
         getMetaClass().b = new B()
         B b = b
      """
    }
}