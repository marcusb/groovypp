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

public class CastTest extends GroovyShellTestCase {

  void testCoerceObject() {
    def res = shell.evaluate("""
@Typed
def m () {
  [1,2,3] as Set
}

m()
        """)

    assertTrue res instanceof Set
  }

  void testCoercePrimitive() {
    def res = shell.evaluate("""
@Typed
def m () {
  1 as Long
}

m()
        """)
    assertTrue res instanceof Long
  }

  void testNumber() {
    def res = shell.evaluate("""
@Typed
def m () {
  (Number)0
}

m()
        """)
    assertTrue res instanceof Integer
  }

  void testNoCoerce() {
    def res = shell.evaluate("""
@Typed
def m () {
  def u = (Map<String,Object>)[:]
  u.put("k","v")
  u
}

m()
        """)
    assertEquals([k: "v"], res)
  }

  void testPrimitiveNoCoerce() {
    def res = shell.evaluate("""
@Typed
def m () {
  Object u = (byte)3;
  [(long)1, (int)u]
}

m()
        """)
    assertEquals([1L, 3], res)

  }

  void testNoCoerceWithInference() {
    def res = shell.evaluate("""
@Typed
def m () {
  def u = (byte)3;
  [(long)1, (int)u]
}

m()
        """)
    assertEquals([1L, 3], res)
  }

  void testCastNullToString() {
    shell.evaluate("""
    @Typed
    class C {
      static def foo() {
         def reader = new StringReader("")
         String line
         while ((line = reader.readLine()) != null) {
         }
      }
    }
    C.foo()
    """)
  }
}