package groovy

class IfElseTest extends GroovyShellTestCase {

    void testIf_NoElse() {
      shell.evaluate  """
          @Typed
          def u() {
            def x = false

            if ( true ) {
                x = true
            }

            assert x == true
          }

          u()
      """
    }

    void testIf_WithElse_MatchIf() {
      shell.evaluate  """

          @Typed
          def u() {
            def x = false
            def y = false

            if ( true ) {
                x = true
            } else {
                y = true
            }

            assert x == true
            assert y == false
          }

          u()
        """
    }

    void testIf_WithElse_MatchElse() {
      shell.evaluate  """

          @Typed
          def u() {
            def x = false
            def y = false

            if ( false ) {
                x = true
            } else {
                y = true
            }

            assert false == x
            assert true == y

          }

          u()
      """
    }

    void testIf_WithElseIf_MatchIf() {
      shell.evaluate  """

          @Typed
          def u() {
            def x = false
            def y = false

            if ( true ) {
                x = true
            } else if ( false ) {
                y = true
            }

            assert x == true
            assert y == false

          }

          u()
        """
    }

    void testIf_WithElseIf_MatchElseIf() {
      shell.evaluate  """

          @Typed
          def u() {
            def x = false
            def y = false

            if ( false ) {
                x = true
            } else if ( true ) {
                y = true
            }

            assert false == x
            assert true == y

          }

          u()
      """
    }

    void testIf_WithElseIf_WithElse_MatchIf() {
      shell.evaluate  """

          @Typed
          def u() {
            def x = false
            def y = false
            def z = false

            if ( true ) {
                x = true
            } else if ( false ) {
                y = true
            } else {
                z = true
            }

            assert x == true
            assert y == false
            assert false == z

          }

          u()
        """
    }

    void testIf_WithElseIf_WithElse_MatchElseIf() {
      shell.evaluate  """

          @Typed
          def u() {
            def x = false
            def y = false
            def z = false

            if ( false ) {
                x = true
            } else if ( true ) {
                y = true
            } else {
                z = true
            }

            assert false == x
            assert true == y
            assert false == z

          }

          u()
        """
    }

    void testIf_WithElseIf_WithElse_MatchElse() {
      shell.evaluate  """

          @Typed
          def u() {
            def x = false
            def y = false
            def z = false

            if ( false ) {
                x = true
            } else if ( false ) {
                y = true
            } else {
                z = true
            }

            assert false == x
            assert y == false
            assert true == z
          }

          u()
        """
    }
}
