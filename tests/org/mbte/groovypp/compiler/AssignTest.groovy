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
  protected int a

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

  void testAssignField() {
    def res = shell.evaluate("""
@Typed
class A {
  protected int a

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

  void testAssignFinalVar() {

  }

  void testAssignFinalField() {

  }
}