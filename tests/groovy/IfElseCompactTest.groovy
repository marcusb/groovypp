package groovy

class IfElseCompactTest extends GroovyShellTestCase {

  void testIf_NoElse() {
    shell.evaluate """

          @Typed(debug=true)
          def u() {
            def x = false
            if ( true ) {x = true}
            assert x == true
          }

          u()
      """
  }

  void testIf_WithElse_MatchIf() {
    shell.evaluate """

          @Typed
          def u() {
            def x = false
            def y = false

            if ( true ) {x = true} else {y = true}

            assert x == true
            assert y == false
          }

          u()
      """
  }
}
