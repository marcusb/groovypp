package groovy

class SimplePostfixTest extends GroovyShellTestCase {

    void testPostfix() {
       shell.evaluate  """

        @Typed
        def u() {
          def x = 1
          ++x
          assert x == 2
        }

        u()
      """
    }

}