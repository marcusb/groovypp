package org.mbte.groovypp.compiler

public class StaticMethodTest extends GroovyShellTestCase {
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