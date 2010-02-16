package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue125Test extends GroovyShellTestCase {
    void testMe () {
       shell.evaluate """
         @Typed(TypePolicy.STATIC)
         package test

         class A {
           def foo (B b) {}
         }
         class B {
           def bar () {
             { -> new A().foo(this) }
           }
         }

         new B().bar()
      """
    }
}