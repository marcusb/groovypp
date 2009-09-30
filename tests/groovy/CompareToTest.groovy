package groovy

class CompareToTest extends GroovyShellTestCase {

    void testCompareTo() {
      shell.evaluate("""
        @Compile
        def u() {
          def a = 12
          def b = 20
          def c = 30

          def result = a <=> b
          assert result < 0

          result = a <=> 12
          assert result == 0

          result = c <=> b
          assert result > 0

          assert (a <=> b) < 0
          assert a <=> 12 == 0
          assert (c <=> b) > 0
        }
        u();
      """
      )
    }

    void testNullCompares() {
      shell.evaluate("""
        @Compile
        def u() {
          def a = 123
          def b = null

          def result = a <=> b
          assert result > 0

          result = b <=> a
          assert result < 0

          result = b <=> null
          assert result == 0
        }
        u();
      """
      )
    }
}
