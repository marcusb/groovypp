package groovy
/**
 * Test case for using the "as" keyword to convert between strings
 * and numbers in both directions.
 */
class AsTest extends GroovyShellTestCase {

    def subject
    /**
     * Test that "as String" works for various types.
     */
    void testAsString() {
      shell.evaluate("""
        @Compile
        def u() {
          assert (48256846 as String) == "48256846"
          assert (0.345358 as String) == "0.345358"
          assert (12.5432D as String) == "12.5432"
          assert (3568874G as String) == "3568874"
        }
        u();
      """
      )
    }

    void testStringAsBigInteger() {
      shell.evaluate("""
        @Compile
        def u() {
          def subject = "34587203957357" as BigInteger
          assert subject.class == BigInteger
          assert subject == 34587203957357
        }
        u();
      """
      )
    }

    void testStringAsLong() {
      shell.evaluate("""
        @Compile
        def u() {
          def subject = "32498687" as Long
          assert subject.class == Long
          assert subject == 32498687L
        }
        u();
      """
      )
    }

    void testStringAsInt() {
      shell.evaluate("""
        @Compile
        def u() {
          def subject = "32498687" as int
          assert subject.class == Integer
          assert subject == 32498687
        }
        u();
      """
      )
    }

    void testStringAsShort() {
      shell.evaluate("""
        @Compile
        def u() {
          def subject = "13279" as Short
          assert subject.class == Short
          assert subject == 13279
        }
        u();
      """
      )
    }

    void testStringAsByte() {
      shell.evaluate("""
        @Compile
        def u() {
          def subject = "12" as Byte
          assert subject.class == Byte
          assert subject == 12
        }
        u();
      """
      )
    }

    void testStringAsBigDecimal() {
      shell.evaluate("""
        @Compile
        def u() {
          def subject = "12.54356" as BigDecimal
          assert subject.class == BigDecimal
          assert subject == 12.54356
        }
        u();
      """
      )
    }

    void testStringAsDouble() {
      shell.evaluate("""
        @Compile
        def u() {
          def subject = "1.345" as double
          assert subject.class == Double
          assert subject == 1.345
        }
        u();
      """
      )
    }

    void testStringAsFloat() {
      shell.evaluate("""
        @Compile
        def u() {
          def subject = "1.345" as float
          assert subject.class == Float
          assert subject == 1.345F
        }
        u();
      """
      )
    }
    
    void testFloatAsBigDecimal() {
      shell.evaluate("""
        @Compile
        def u() {
          def subject = 0.1f as BigDecimal
          assert subject.class == BigDecimal
          assert subject == 0.1
        }
        u();
      """
      )
    }
    
    void testDoubleAsBigDecimal() {
      shell.evaluate("""
        @Compile
        def u() {
          def subject = 0.1d as BigDecimal
          assert subject.class == BigDecimal
          assert subject == 0.1
        }
        u();
      """
      )
    }
    
    void testFloatAsDouble() {
      shell.evaluate("""
        @Compile
        def u() {
          def subject = 0.1f as Double
          assert subject.class == Double
          assert subject == 0.1
        }
        u();
      """
      )
    }
}
