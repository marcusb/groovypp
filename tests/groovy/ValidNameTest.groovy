package groovy

class ValidNameTest extends GroovyShellTestCase {

    void testFieldNamesWithDollar() {
      def script = """
        @Typed
        def u() {
          def \$foo\$foo = '3foo'
          def foo\$foo = '2foo'
          def \$foo = '1foo'
          def foo = '0foo'
          assert \$foo\$foo == '3foo'
          assert foo\$foo == '2foo'
          assert \$foo == '1foo'
          assert foo == '0foo'
          assert "\$foo\$foo\${foo\$foo}\${\$foo\$foo}\${\$foo}\$foo" == '0foo0foo2foo3foo1foo0foo'
        }

        u()
      """
      shell.evaluate script
    }

    void testClassAndMethodNamesWithDollar() {
          shell.evaluate(
            """
              @Typed
              class \$Temp {
                  def \$method() { 'bar' }
              }
              @Typed
              def u() {
                  assert new \$Temp().\$method() == 'bar'
              }

              u()
            """
          );

    }
}