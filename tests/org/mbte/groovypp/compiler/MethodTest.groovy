package org.mbte.groovypp.compiler

public class MethodTest extends GroovyShellTestCase {

  void testListPlus () {
    def res = shell.evaluate ("""
@CompileStatic
def m () {
  [[1, 2, 3] + [4, 5], (([2, 3, 1, 4] as Set) - 4).sort() ]
}

m ()
    """)
    assertEquals ([[1,2,3,4,5],[1,2,3]], res)

  }

  void testDgm () {
    def res = shell.evaluate ("""
@CompileStatic
def m () {
  [1, 2, 3, 4, 5, 6].each { 
    println it
  }
}

m ()
    """)
  }

  void testIface () {
      def res = shell.evaluate ("""
interface I {
  int oneMethod (List list1, List list2)
}

int method(List l1, List l2, I i) {
   i.oneMethod(l1, l2)
}

@CompileStatic
int test () {
   method([1, 2], [3, 4, 5]) {
      List l1, List l2 ->
       l1.size () + l2.size ()
   }
}

test ()
      """)
      println res
      assertEquals (5, res)
  }


    void testMethod() {
        shell.evaluate """
class X {
  int method (int value) {
    value
  }

  @CompileStatic
  def u () {
     v(3)
     assert 5 == method((int)v(5))
  }

  @CompileStatic
  static long v (int u) {
    u
  }
}

    println new X ().u ()
    """
    }

    void testStaticMethod() {
        def res = shell.evaluate("""
        class U239 {
          static method () {
            239
          }

          def method1 () {
          }

          @CompileStatic
          static method2 () {
            U239.method()
          }
        }

        U239.method2()
    """
        )

        assertEquals 239, res
    }
}