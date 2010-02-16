package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue125Test extends GroovyShellTestCase {
    void testMe () {
       shell.evaluate """
         @Typed(TypePolicy.STATIC)
         package test

         class A {
           def foo (B b) {b.f}
         }
         class B {
           int f
           def bar () {
             f = 123;
             { -> new A().foo(this) }()
           }
         }

         assert 123 == new B().bar()
      """
    }
}