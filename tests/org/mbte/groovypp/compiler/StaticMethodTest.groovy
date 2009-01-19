package org.mbte.groovypp.compiler

public class StaticMethodTest extends GroovyShellTestCase {
    void testQualifiedStaticMethod () {
        def res = shell.evaluate("""
@Compile
def u (int d) {
  [0 + d, 30 + d, 60 + d, 90 + d].collect { int rr ->
    (int)(Math.sin((rr-d)*Math.PI/180d)*100+0.5d)
  }
}

u (3)
""")
        assertEquals ([0, 50, 87, 100], res)
    }

    void testStatic () {
        def res = shell.evaluate("""
@Compile(debug=true)
class A {
  def u = 10

  def uu () {
      [0,1,2].collect {
        u
      }
  }
}

new A().uu ()
""")
        assertEquals ([10, 10, 10], res) 
    }
}