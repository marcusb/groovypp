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

public class AssignTest extends GroovyShellTestCase {

  void testVarAssign() {
    def res = shell.evaluate("""
@Typed
def u () {
  int x = 0
  (x = 10) + x
}
u ()
""")
    assertEquals 20, res
  }

  void testVarAssignPlus() {
    def res = shell.evaluate("""
@Typed
def u () {
  def x = 20
  x -= 10

  def y = x
  y += "30"

  [x, y]
}
u ()
""")
    assertEquals([10, "1030"], res)
  }

  void testArrayAssign() {
    def res = shell.evaluate("""
@Typed
def u (int [] x) {
  (x [1] = 10) + x [1]
}
u (new int [10])
""")
    assertEquals 20, res
  }

  void testArrayAssignOp() {
    def res = shell.evaluate("""
@Typed
def u (int [] x) {
   x [0] = 1
   x [1] = 6
   x [1] += (x [x[0]] |= 1)
}
u (new int [10])
""")
    assertEquals 13, res
  }

    void testCollectionAssignOp() {
      def res = shell.evaluate("""
  @Typed
  def u (List<Integer> x) {
     x [0] += 6
  }
  u ([1])
  """)
      assertEquals (7, res)
    }

  void testArrayAssignViaProperty() {
    def res = shell.evaluate("""
@Typed
class A {
  int [] a

  def u (int [] aa) {
     a = aa
    (0..9).each { int it ->
        a [it] = it
    }
    a as List
  }
}

new A().u (new int[10])
""")
    assertEquals(0..9, res)
  }

  void testAssignProperty() {
    def res = shell.evaluate("""
@Typed
class A {
  int a

  A u () {
    (0..10).each { int it ->
        a = it
    }
    this
  }
}

new A().u ().a
""")
    assertEquals 10, res
  }

  void testAssignPropertyOp() {
    def res = shell.evaluate("""
@Typed
class A {
  int a

  A u () {
    (0..10).each { int it ->
        a += it
    }
    this
  }

  void setA (int v) {
    this.@a = v
  }
}

new A().u ().a
""")
    assertEquals 55, res
  }

  void testAssignField() {
    def res = shell.evaluate("""
@Typed
class A {
  protected int a
  protected int b

  A u () {
    (0..4).each { int it ->
        a += it
        (0..it).each { int jt ->
           b += a
        }
    }
    this
  }
}

new A().u ()
""")
    assertEquals 10, res.a
    assertEquals 85, res.b
  }

  void testAssignFieldOp() {
    def res = shell.evaluate("""
@Typed
class A {
  protected int a

  A u () {
    (0..10).each { int it ->
        a += it
    }
    this
  }
}

new A().u ().a
""")
    assertEquals 55, res
  }

  void testAssignFinalVar() {

  }

  void testAssignFinalField() {

  }

  void testVar() {
    def types = ["int", "Integer", "byte", "Byte",
            "short", "Short", "long", "Long", "float", "Float", "double", "Double",
            "char", "Character", "BigInteger", "BigInteger"]

    types.each {jt ->
      types.each {it ->
        println "$it val = ($jt)5"
        shell.evaluate("""
      @Typed
      def U () {
        $it val = ($jt)5
      }
        """
        )
      }
    }
  }

  // Setter should have 'void' return type, whereas here we have 'Object'.
  // Shouldn't crash.
  void testNotASetter() {
    assertNotNull(shell.evaluate("""
      @Typed class C {
         Object result
         def setResult(Object result) {
           this.result = result
         }
      }
      new C()
    """))
  }
}