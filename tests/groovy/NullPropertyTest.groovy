package groovy

/**
 * @author Jeremy Rayner 
 */
class NullPropertyTest extends GroovyShellTestCase { 
    def wensleydale = null

    void testNullProperty() {
         shell.evaluate  """

          class A {
            def foo = null;
            def u() {
              assert foo == null;
            }
          }
          new A().u()
        """
    } 
} 


