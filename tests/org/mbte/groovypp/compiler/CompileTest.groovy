package org.mbte.groovypp.compiler

import static groovy.CompileTestSupport.shouldCompile

public class CompileTest extends GroovyShellTestCase {

  void testProp() {
    def res = shell.evaluate("""
@Typed
class X {
  Integer prop

  def m () {
    [prop ? prop : 0L, prop?: 0d]
  }
}

new X (prop:10).m ()
      """)
    println res
    assertEquals([10L, 10d], res)
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

  void testIDef() {
//        def res = shell.evaluate ("""
//@Typed
//class A extends GroovyTestCase {
//
//  @IDef
//  def testMethod (int a) {
//    a*a
//  }
//
//  def testMe (int a) {
//    List res = [testMethod(a)]
//    setTestMethod { int b ->
//      10*b
//    }
//    res.add(testMethod(a))
//    res
//  }
//}
//
//new A().testMe (4)
//        """)
//        println res
//        assertEquals ([16, 40], res)
  }

  void testClass() {
    def res = shell.evaluate("""
@Typed
class A {
  static def doIt (int a = 0, int b = 5){
    a + b
  }

  def test () {
    doIt(3)
  }

  def list () {
     { int a = 2, int b = 3 -> a + b }
  }
}

def a = new A()
[a.test (), a.list().call(), a.list().call(1), a.list().call(1,2)]
        """)
    println res
    assertEquals([8, 5, 4, 3], res)
  }

  void testClosure() {
    def res = shell.evaluate("""
def v (Closure cl) {
  cl.call()
}

@Typed
def u () {
   List s = [1, 2, 3, 4]

   v {
     List n = s
     v {
         n.addAll(s)
         n
     }
     n
   }
}

u()
        """)

    assertEquals([1, 2, 3, 4, 1, 2, 3, 4], res)
  }

  void testAssert() {
    shouldFail(AssertionError) {
      shell.evaluate """
    @Typed
    def u () {
       assert 4

       assert 6 == 5, "BUG"
    }

    u ()
      """
    }
  }

  void testAssert2() {

    shouldFail(AssertionError) {
      println(shell.evaluate("""
    @Typed
    def u () {
       assert (!(12 && 1L))  || ("XXX" && 0)
    }

    u ()
      """
      ))
    }
  }

  void testList() {
    def res = shell.evaluate("""
    @Typed
    def u () {
       [1, *[false,7], null, *[], *[3,4,5]]
    }

    u ()
      """
    )

    assertEquals([1, false, 7, null, 3, 4, 5], res)
  }

  void testMap() {
    def res = shell.evaluate("""
      @Typed
      def u () {
         [a:1, b:2, c:3, d:4, false:5]
      }

      u ()
        """
    )

    Map expected = [a: 1, b: 2, c: 3, d: 4, false: 5]
    assertEquals(expected, res)
  }

  void testIf() {
    def res = shell.evaluate("""
      @Typed
      def u (val) {
         if (val == true)
           "true"
         else {
           if(val == false)
              "not true"
           else
             val
         }
      }

      [u (true), u(false), u("abc")]
        """
    )

    assertEquals(["true", "not true", "abc"], res)
  }

  void testEmptyRange() {
    def res = shell.evaluate("""
      @Typed
      def u() {
        def ret = []
        for(i in 0..<0) {
          ret << i
        }
        ret
      }
      u()
    """)
    assertEquals([], res)
  }

  void testIterateIntRange() {
    def res = shell.evaluate("""
      @Typed
      def u() {
        def ret = []
        for(i in 1..<-1) {
          ret << i
          println i
        }
        ret
      }
      u()
    """)
    assertEquals([1, 0], res)
  }

  void testDirectFunctionCall() {
    def fact = shell.evaluate("""
      @Typed
      def u(int n) {
        Function1<Integer, Integer> f = {
          i -> if (i < 2) 1
               else i * call(i-1)
        }
        f(n)
      }
      u(5)
    """)
    assertEquals(120, fact)
  }

  void testThreadCreate() {
    shouldCompile("""
      @Typed
      class C {
        static def foo() {Thread t = [run: {}]}
      }
      C.foo()
    """)
  }
  void testClassExpression() {
    shell.evaluate("""
    @Typed
    class C {
      static def foo() {C.class.getClassLoader()}
    }
    C.foo()
    """)
  }

}