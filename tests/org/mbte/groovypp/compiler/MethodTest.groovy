package org.mbte.groovypp.compiler

public class MethodTest extends GroovyShellTestCase {

  void testSubclass() {
    def res = shell.evaluate("""
    @Typed
  abstract class A {
      abstract define ()

      void onMessage (Runnable r) {
        r.run ()
      }

      def static act(A a) {
         a.define ()
      }

      static def test() {
        def out = []
        act {
           onMessage {
             out << 239
           }
           out
        }
      }
  }
A.test ()
        """)
    assertEquals([239], res)
  }

  void testNullParam() {
    def res = shell.evaluate("""
  @Typed
class A {
  def u (int msg, Closure when) {
    if (when)
      msg + (int)when.call ()
    else
      msg
  }

  def test () {
    [ u(2, null), u(3, {4}) ]
  }
}

new A().test ()
      """)
    assertEquals([2, 7], res)

  }



  void testListPlus() {
    def res = shell.evaluate("""
@Typed
def m () {
  [[1, 2, 3] + [4, 5], (([2, 3, 1, 4] as Set) - 4).sort() ]
}

m ()
    """)
    assertEquals([[1, 2, 3, 4, 5], [1, 2, 3]], res)

  }

  void testDgm() {
    def res = shell.evaluate("""
@Typed
def m () {
  [1, 2, 3, 4, 5, 6].each { 
    println it
  }
}

m ()
    """)
  }

  void testIface() {
    def res = shell.evaluate("""
interface I {
  int oneMethod (List list1, List list2)
}

int method(List l1, List l2, I i) {
   i.oneMethod(l1, l2)
}

@Typed
int test () {
   method([1, 2], [3, 4, 5]) {
      List l1, List l2 ->
       l1.size () + l2.size ()
   }
}

test ()
      """)
    println res
    assertEquals(5, res)
  }


  void testMethod() {
    shell.evaluate """
class X {
  int method (int value) {
    value
  }

  @Typed
  def u () {
     v(3)
     assert 5 == method((int)v(5))
  }

  @Typed
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

          @Typed
          static method2 () {
            U239.method()
          }
        }

        U239.method2()
    """
    )

    assertEquals 239, res
  }

  void testSafe() {
    def res = shell.evaluate("""
          @Typed(debug=true)
          def u () {
             String x = null, y = "null"
             if (!x?.equals("abc"))
               [x?.getChars(), x?.chars, y?.substring(1), y?.chars?.getAt([0,1])]
             else
               []
          }
          u ()
          """)
    assertEquals([null, null, "ull", ['n', 'u']], res)
  }

}