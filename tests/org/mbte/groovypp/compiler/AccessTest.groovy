package org.mbte.groovypp.compiler

import static groovy.CompileTestSupport.shouldCompile
import static groovy.CompileTestSupport.shouldNotCompile
/**
 * @author ven
 */
class AccessTest extends GroovyShellTestCase {
  void testPrivateMethod() {
    shouldNotCompile """
      @Typed
      void foo(Class c) {
        c.initAnnotationsIfNecessary()
      }
    """
  }

  void testPrivateField() {
    shouldNotCompile """
      @Typed
      void foo(Class c) {
        c.publicFields
      }
    """
  }

  void testInaccessibleClone() {
    shouldNotCompile """
      @Typed
      class Box<A extends Comparable<A> & Cloneable> {
        A a;
        Box(A a) {
          this.a = a;
        }

        int foo(A param) {
           assert a == a.clone()

           a.compareTo(param)
        }
      }

      @Typed
      def u() {
        def box = new Box<Date>(new Date());
        assert 1 == box.foo(new Date(10000));
      }
      u()
     """
  }

  void testOuterPrivate() {
    shouldCompile("""
    @Typed class Outer {
       private int i
       class Inner {
         def foo() {
           int j = i
         }
       }
    }
    0
    """)
  }
}
