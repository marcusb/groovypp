package org.mbte.groovypp.compiler

public class TypeInferenceTest extends GroovyShellTestCase {

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
}