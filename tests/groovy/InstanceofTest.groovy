package groovy

class InstanceofTest extends GroovyShellTestCase {

    void testTrue() {
      shell.evaluate  """

          @Typed(debug=true)
          def u() {
            def x = false
            def o = 12

            if ( o instanceof Integer ) {
                x = true
            }

            assert x == true
          }

          u()
        """
    }
    
    void testFalse() {
      shell.evaluate  """

          @Typed
          def u() {
            def x = false
            def o = 12

            if ( o instanceof Double ) {
                x = true
            }

            assert x == false

          }

          u()
        """
    }
    
    void testImportedClass() {
        shell.evaluate  """

          @Typed
          def u() {
            def m = ["xyz":2]
            assert m instanceof Map
            assert !(m instanceof Double)

          }

          u()
        """
    }
    
    void testFullyQualifiedClass() {
        shell.evaluate  """

          @Typed
          def u() {
            def l = [1, 2, 3]
            assert l instanceof java.util.List
            assert !(l instanceof Map)
            
          }

          u()
        """
    }
    
    void testBoolean(){
        shell.evaluate  """

          @Typed
          def u() {
             assert true instanceof Object
             assert true==true instanceof Object
             assert true==false instanceof Object
             assert true==false instanceof Boolean
             assert !new Object() instanceof Boolean
          }

          u()
        """
    }
}
