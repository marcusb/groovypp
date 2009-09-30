package groovy

/**
 * @version $Revision: 13550 $
 */
class CompareTypesTest extends GroovyShellTestCase {
    void testCompareByteToInt() {
      shell.evaluate("""
        @Compile
        def u() {
          Byte a = 12
          Integer b = 10

          assert a instanceof Byte
          assert b instanceof Integer

          assert a > b
        }
        u();
      """
      )
    }
    
    void testCompareByteToDouble() {
      shell.evaluate("""
        @Compile
        def u() {
          Byte a = 12
          Double b = 10

          assert a instanceof Byte
          assert b instanceof Double

          assert a > b
        }
        u();
      """
      )

    }
     
    void testCompareLongToDouble() {
      shell.evaluate("""
        @Compile
        def u() {
          Long a = 12
          Double b = 10

          assert a instanceof Long
          assert b instanceof Double

          assert a > b
        }
        u();
      """
      )
    } 
     
    void testCompareLongToByte() {
      shell.evaluate("""
        @Compile
        def u() {
          Long a = 12
          Byte b = 10

          assert a instanceof Long
          assert b instanceof Byte

          assert a > b

        }
        u();
      """
      )

    }
     
    void testCompareIntegerToByte() {
      shell.evaluate("""
        @Compile
        def u() {
          Integer a = 12
          Byte b = 10

          assert a instanceof Integer
          assert b instanceof Byte

          assert a > b

        }
        u();
      """
      )

    }
    
    void testCompareCharToLong() {
      shell.evaluate("""
        @Compile
        def u() {
          def a = Integer.MAX_VALUE
          def b = ((long) a)+1
          a=(char) a

          assert a instanceof Character
          assert b instanceof Long

          assert a < b
        }
        u();
      """
      )
    }
    
    void testCompareCharToInteger() {
      shell.evaluate("""
        @Compile
        def u() {
          Character a = Integer.MAX_VALUE
          Integer b = a-1

          assert a instanceof Character
          assert b instanceof Integer

          assert a > b
        }
        u();
      """
      )
    }
} 


