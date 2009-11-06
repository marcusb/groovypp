package groovy

class PostfixTest extends GroovyShellTestCase {

  void testIntegerPostfix() {
    shell.evaluate """

          @Typed
          def u() {
            def x = 1

            def y = x++

            assert y == 1
            assert x == 2

            assert x++ == 2
            assert x == 3
          }

          u()
        """
  }

  /*
   This is actually test with BigDecimal,
   but I leave method name to stay consistent
   with groovy tests
  */

  void testDoublePostfix() {
    shell.evaluate """

          @Typed
          def u() {
            def x = 1.2
            def y = x++

            assert y == 1.2
            assert x++ == 2.2
            assert x == 3.2
          }

          u()
        """
  }

  void testStringPostfix() {
    shell.evaluate """

          @Typed
          def u() {
             def x = "bbc"
             x++

             assert x == "bbd"

             def y = "bbc"++
             assert y == "bbc"
          }

          u()
        """
  }



  /**
   * Not sure if this actually should work in @Typed method.
   */
  void testArrayPostfix() {
    shell.evaluate """

          @Typed
          def u() {
            int[] i = [1]

            def y = i[0]++

            assert y == 1
            assert i[0]++ == 2
            assert i[0] == 3
          }

          u()
        """
  }

  void testConstantPostfix() {
    shell.evaluate """

          @Typed
          def u() {
            assert 1 == 1++
          }

          u()
        """
  }



  void testFunctionPostfix() {
    shell.evaluate """
          int valueReturned() { 0 }

          @Typed
          def u() {
            def z = (valueReturned())++
            assert z == 0
          }

          u()
        """
  }

  void testPrefixAndPostfix() {
    shell.evaluate """

          def v() {
            def u = 0

//            assert -1 == -- u --
//            assert 0 == ++ u ++
//            assert 0 == u
            assert 0 == (u++)++
            assert 2 == u
          }

          v()
        """
  }
}
