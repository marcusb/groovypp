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

package groovy

import static groovy.CompileTestSupport.shouldCompile
import static groovy.CompileTestSupport.shouldNotCompile

class ForInTest extends GroovyShellTestCase {

  void testForInList() {
    def res = shell.evaluate("""
      @Typed
      public def foo(List<String> l) {
        def res = []
        for (el in l) res << el.toLowerCase()
        res
      }
      foo(["Es", "GEHT"])
    """)
    assertEquals(["es", "geht"], res)
  }
  void testForInIntRange() {
    def res = shell.evaluate("""
      @Typed
      public def foo() {
        def res = []
        for (i in 0..2) res << i.byteValue()
        res
      }
      foo()
    """)
    assertEquals([0,1,2], res)
  }

   void testForInIntRangeTypeSpecified() {
    def res = shell.evaluate("""
      @Typed
      public def foo() {
        def res = []
        for (Integer i in 0..2) res << i.byteValue()
        res
      }
      foo()
    """)
    assertEquals([0,1,2], res)
  }

  void testForInIntArray() {
    def res = shell.evaluate("""
      @Typed
      public def foo() {
        def res = []
        int[] arr = [0,1,2]
        for (i in arr) res << i.byteValue()
        res
      }
      foo()
    """)
    assertEquals([0,1,2], res)
  }

  void testForIterator() {
    def res = shell.evaluate("""
      @Typed
      public def foo() {
        def res = []
        def it = [0,1,2].iterator()
        for (i in it) res << i.byteValue()
        res
      }
      foo()
    """)
    assertEquals([0,1,2], res)
  }

  void testForReader() {
    def res = shell.evaluate("""
      @Typed
      public def foo(Reader r) {
        def res = []
        for (str in r) res << str.toLowerCase()
        res
      }
      foo(new StringReader("Schwiitzi\\nNati"))
    """)
    assertEquals(["schwiitzi", "nati"], res)
  }

}