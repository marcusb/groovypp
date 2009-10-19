package groovy

/** 
 * Tests the use of classes as variable expressions
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision: 4996 $
 */
class ClassExpressionTest extends GroovyShellTestCase {

    void testUseOfClass() {
      shell.evaluate("""
          @Typed
          class A {
            A() {
              def x = A
              assert x != null
            }
          }

          @Typed
          def u() {
            def x = String

            assert x != null

            assert x.getName().endsWith('String')
            assert x.name.endsWith('String')

            x = Integer

            assert x != null
            assert x.name.endsWith('Integer')

            x = GroovyTestCase

            assert x != null
            assert x.name.endsWith('GroovyTestCase')

            new A()
          }
          u();
        """
      )

    }

    void testClassPsuedoProperty() {
      shell.evaluate("""
          @Typed
          def u() {
            def x = "cheese";
            assert x.class != null
            assert x.class == x.getClass();
          }
          u();
        """
      )
    }
    
    void testPrimitiveClasses() {
      shell.evaluate("""
          @Typed
          def u() {
            assert void == Void.TYPE
            assert int == Integer.TYPE
            assert byte == Byte.TYPE
            assert char == Character.TYPE
            assert double == Double.TYPE
            assert float == Float.TYPE
            assert long == Long.TYPE
            assert short == Short.TYPE
          }
          u();
        """
      )
    }
    
    void testArrayClassReference() {
      shell.evaluate("""
          @Typed
          def u() {
            def foo = int[]
            assert foo.name == "[I"
          }
          u();
        """
      )
    }
}
