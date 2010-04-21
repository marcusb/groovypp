/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.groovypp.compiler

import static groovy.CompileTestSupport.shouldNotCompile

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

  void testInaccessibleSameFileField() {
    shouldNotCompile """
      @Typed
      class C { private static int i }
      @Typed
      class D { private static int j = C.i }
    """
  }

  void testInaccessibleSameFileMethod() {
    shouldNotCompile """
      @Typed
      class C { private static int f() {0} }
      @Typed
      class D { private static int j = C.f() }
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

  void testOuterPrivateField() {
    def res = shell.evaluate("""
    @Typed class Outer {
       private int i
       class Inner {
         def foo() {
           print i
           i = 6
         }
       }
       def foo() {
         new Inner().foo()
       }
    }
    new Outer().foo()
    """)
    assertEquals 6, res
  }
  void testOuterPrivateMethod() {
    def res = shell.evaluate("""
    @Typed class Outer {
       private int i() {7}
       class Inner {
         def foo() {
           i()
         }
       }
       def foo() {
         new Inner().foo()
       }
    }
    new Outer().foo()
    """)
    assertEquals 7, res
  }

  void testOuterPrivateConstructor() {
    shell.evaluate("""
    @Typed class Outer {
       private Outer() {}
       class Inner {
         def foo() {
           new Outer()
         }
       }
       def foo() {
         new Inner().foo()
       }
    }
    new Outer().foo()
    """)
  }

}
