package org.mbte.groovypp.compiler

public class InnerTest extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Typed
class A {
   static def field = 10
   void main (a) {
     new C ().r ()
   }

   class C {
      def r () {
        4.times { it ->
          3.times {
            new B(it).u (it)
          }
        }
      }
   }

   class B {
     def s
     B (s) { this.s = s}
     def u (i) { println i }
   }}

   static class D { def u () { field = field + 12 } } """
    }
}