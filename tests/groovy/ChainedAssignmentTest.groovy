package groovy

class ChainedAssignmentTest extends GroovyShellTestCase {


    void testCompare() {
      shell.evaluate("""
          @Typed
          def dummy(v) {
              print v
          }

          @Typed
          def u() {
            def i = 123
            def s = "hello"

            def i2
            def i1 = i2 = i;
            assert i1 == 123
            assert i2 == 123

            def s1
            dummy(s1 = s)
            assert s1 == "hello"
          }
          u();
        """
      )
    }
}
