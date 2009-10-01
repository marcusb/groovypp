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
  def u = (Serializable)[:]
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
}