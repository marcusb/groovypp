package org.mbte.groovypp.compiler

public class AssignTest extends GroovyShellTestCase {

    void testVarAssign () {
        def res = shell.evaluate("""
@Compile
def u () {
  int x = 0
  (x = 10) + x
}
u ()
""")
        assertEquals 20, res
    }

    void testArrayAssign () {
        def res = shell.evaluate("""
@Compile(debug=true)
def u (int [] x) {
  (x [1] = 10) + x [1]
}
u (new int [10])
""")
        assertEquals 20, res
    }

    void testArrayAssignViaProperty () {

    }

    void testAssignProperty () {

    }

    void testAssignFinalVar () {

    }

    void testAssignFinalField () {

    }
}