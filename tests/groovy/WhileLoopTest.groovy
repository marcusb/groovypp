package groovy

class WhileLoopTest extends GroovyShellTestCase {

    void testVerySimpleWhile() {
      def res = shell.evaluate("""
        @Compile
        def u() {
          def x = 0;
          def m = 5;
          while ( x < m ) {
              x = x + 1
          }

          return x
        }
        u();
      """
      )
      assertEquals(5, res); 
    }

    void testWhileWithEmptyBody() {
      def res = shell.evaluate("""
        @Compile
        def u() {
          int x = 3
          while (--x);
          x
        }
        u();
      """
      )

      assertEquals (0, res)
    }

    void testMoreComplexWhile() {
      shell.evaluate("""
        @Compile
        def u() {
          def x = 0
          def y = 5

          while ( y > 0 ) {
              x = x + 1
              y = y - 1
          }

          assert x == 5
        }
        u();
      """
      )
    }
}
