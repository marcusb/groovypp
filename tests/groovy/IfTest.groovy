package groovy

class IfTest extends GroovyShellTestCase {

    void testUsingNumber() {
        shell.evaluate  """

          @Typed(debug=true)
          def u() {
            def x = 1

            if (x) {
            } else {
                assert false
            }

            x = 0
            if (x) {
                assert false
            } else {
            }
          }

          u()
        """

    }

    void testUsingString() {
        shell.evaluate  """

          @Typed
          def u() {
            def x = "abc"

            if (x) {
            } else {
                assert false
            }

            x = ""

            if (x) {
                assert false
            } else {

            }
          }

          u()
        """
    }
}
