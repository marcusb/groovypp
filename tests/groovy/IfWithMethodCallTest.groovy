package groovy

class IfWithMethodCallTest extends GroovyShellTestCase {

    void testIfWithMethodCall() {
        shell.evaluate  """

          @Typed
          def u() {
            def x = ["foo", "cheese"]

            if ( x.contains("cheese") ) {
                // ignore
            } else {
                assert false
            }
          }

          u()
        """
    }
}
