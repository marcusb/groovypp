package org.mbte.groovypp.compiler

public class TypeInferenceTest extends GroovyShellTestCase {

  void testAssert() {
    def res = shell.evaluate("""
@Typed
class A extends GroovyTestCase {
    def m () {
        def list = [] as List<Number>
        list.leftShift 1
        list << 2
        assertEquals ([1,2], list)

        if (list.size() == 2) {
            list = list [0]
            list++
            assertTrue (list instanceof Integer)
        }
        else {
            list = 239G
            assertTrue list instanceof BigDecimal
        }
        list instanceof Number
    }
}

new A().m ()
        """)
    assertTrue res
  }


    void testList() {
      def res = shell.evaluate("""
      @Typed
      def m () {
          def list = [2] as List<Number>
          def u = list.get(0), w = list.getAt(0), v = list[0]
          [u++, w++, v++]
      }
      m ()
          """)
      assertEquals ([2,2,2], res)
    }

    void testArrayInference() {
      def res = shell.evaluate("""
      @Typed class Foo {
        def <T> T getFirst(T[] ts) { ts[0] }
        def bar() {
           int[] arr = new int[1]
           arr[0] = 0
           getFirst(arr) * 100
        }
      }
      new Foo().bar()
          """)
      assertEquals (0, res)
    }

    void testArrayInference1() {
      def res = shell.evaluate("""
      @Typed(debug=true) class Foo {
        def <T> T[] getArray(T[] ts) { ts }
        def bar() {
           int[] arr = new int[1]
           arr[0] = 5
           arr = getArray(arr)
           arr[0]
        }
      }
      new Foo().bar()
          """)
      assertEquals (5, res)
    }

    void testVarargsInference() {
      def res = shell.evaluate("""
        @Typed
        def bar() {
          Arrays.asList("schwiitzi", "nati").get(1).charAt(0)
        }
        bar()
        """)
      assertEquals ('n', res)
    }


  void testCast() {
    def res = shell.evaluate("""
  @Typed
  def m (val) {
    (List)val
    ((List)val).size ()
  }

  m ([1,2,3])
      """)
    assertEquals 3, res
  }

  void testInference() {
    def res = shell.evaluate("""
@Typed
def m () {
   def x = [1, 2, 3]
   x.leftShift(4)
   x = x + 5

   def y
   if (x.size() == 5)
     y = x.size() + 1.0
   else
     y = 5G

   x.leftShift(y.doubleValue ())

   if (!x)
     y = [1] as Set
   else {
     y = [2]
   }

   x.leftShift(y.size ())

   def u = 0
   while (!(u == 10)) {
     u++
   }
   x.add(u)
   x.add "\$u \${->((List)x).size()}"
   x
}

m ()
    """)
    assertEquals([1, 2, 3, 4, 5, 6.0d, 1, 10, "10 9"], res)
  }

  void testListWithGen() {
    def res = shell.evaluate ("""
        @Typed
         <T> List<List<T>> u (List<List<T>> list, T toAdd) {
          for (int i = 0; i != list.size (); ++i)
            list [i] << toAdd
          list
        }
        u ([[0], [1], [2]], -1)
    """)
    assertEquals ([[0, -1], [1,-1], [2,-1]], res)
  }
}
