package groovy

class MultiplyDivideEqualsTest extends GroovyShellTestCase {

    void testIntegerMultiplyEquals() {
      shell.evaluate  """

        @Typed
        def u() {
          def x = 2
          def y = 3
          x *= y

          assert x == 6

          y *= 4

          assert y == 12
        }

        u()
      """
    }

    void testCharacterMultiplyEquals() {
      shell.evaluate  """

        @Typed
        def u() {
          Character x = 2
          Character y = 3
          x *= y

          assert x == 6

          y *= 4
          
          assert y == 12
        }

        u()
      """
    }
    
    void testNumberMultiplyEquals() {
      shell.evaluate  """

        @Typed
        def u() {
          def x = 1.2
          def y = 2
          x *= y

          assert x == 2.4
        }
        u()
      """
    }
    
    void testStringMultiplyEquals() {
        shell.evaluate  """

          @Typed
          def u() {
            def x = "bbc"
            def y = 2
            x *= y

            assert x == "bbcbbc"

            x = "Guillaume"
            y = 0
            x *= y
            assert x == ""
          }

          u()
        """
    }
    
    
    void testIntegerDivideEquals() {
        shell.evaluate  """

          @Typed
          def u() {
            def x = 18
            def y = 6
            x /= y

            assert x == 3.0

            y /= 3

            assert y == 2.0
          }

          u()
        """
    }
    
    void testCharacterDivideEquals() {
        shell.evaluate  """

          @Typed
          def u() {
            Character x = 18
            Character y = 6
            x /= y

            assert x == 3

            y /= 3

            assert y == 2
          }

          u()
        """
    }
    
    void testNumberDivideEquals() {
      shell.evaluate  """

        @Typed
        def u() {
          def x = 10.4
          def y = 2
          x /= y

          assert x == 5.2
        }

        u()
      """
    }
}
