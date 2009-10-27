package org.mbte.groovypp.compiler

public class ClosureTest extends GroovyShellTestCase {

  void testAssignable() {
    def res = shell.evaluate("""
      interface I<T> {
        T calc ()
      }

      @Typed
      def u () {
        I r = {
            11
        }

        def c = (I){
            12
        }

        def u = { 13 } as I

        [r.calc (), c.calc (), u.calc()]
      }

      u ()
  """)
    assertEquals([11, 12, 13], res)
  }

  void testListAsArray() {
    def res = shell.evaluate("""
        interface I<T> {
          T calc ()
        }

        @Typed
        def u () {
          def x = (I[])[ {->10}, {->9} ]

          [x[0].calc(), x[1].calc () ]
        }

        u ()
    """)
    assertEquals([10, 9], res)
  }

  void testArgsCoerce() {
    def res = shell.evaluate("""
        interface I<T> {
          T calc ()
        }

        def v ( I a, I b, I c) {
           a.calc () + b.calc () + c.calc ()
        }

        @Typed
        def u (int add) {
            v ( {10}, {11+add}, {12} )
        }

        u (10)
    """)
    assertEquals(43, res)
  }

  void testMap() {
    def res = shell.evaluate("""
        interface I<T> {
          T calc ()
        }

        class ListOfI extends LinkedList<I> {}

        @Typed
        def u () {
          def     x = [{14}] as List<I>
          def     y = (List<I>)[{15}]
          ListOfI z = [{16}]

          [x[0].calc (), y[0].calc(), z[0].calc () ]
        }

        u ()
    """)
    assertEquals([14, 15, 16], res)
  }

  void testModifyExternalVar() {
    shell.evaluate """
      @Typed(debug=true)
      def u() {
        def sum = [1, 2, 3].each(0){int it, int sum -> sum += it}
        assert sum == 6
      }
      u();
  """
  }

}