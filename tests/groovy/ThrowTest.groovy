package groovy

class ThrowTest extends GroovyShellTestCase {
    
    void testThrow() {
        shell.evaluate  """

          @Typed
          def u() {
            try {
                throw new Exception("abcd")
                assert false;
            }
            catch (Exception e) {
                assert e.message == "abcd"
            }
          }

          u()
        """
    }
}
