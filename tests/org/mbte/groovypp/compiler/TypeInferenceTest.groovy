package org.mbte.groovypp.compiler

public class TypeInferenceTest extends GroovyShellTestCase {

    void testAssert () {
        def res = shell.evaluate ("""
@Compile
class A extends GroovyTestCase {
    def m () {
        def list = []
        list.leftShift 1
        list << 2
        assertEquals ([1,2], list)

        if (list.size() == 2) {
            list = (Number)list [0]
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


  void testCast () {
      def res = shell.evaluate ("""
  @Compile
  def m (val) {
    (List)val
    val.size ()
  }

  m ([1,2,3])
      """)
      assertEquals 3, res
  }

  void testInference () {
    def res = shell.evaluate ("""
@Compile
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
    assertEquals ([1,2,3,4,5,6.0d, 1, 10, "10 9"], res)
  }

  void testSafe () {
  }
}