package org.mbte.groovypp.compiler


public class PromotionTest extends GroovyShellTestCase {
  void testPromo1() {
    shell.evaluate """
    @Typed
    def u () {
      def C = '1'.toCharacter()
      def B = new Byte("1")
      assert (C + B instanceof Integer)
    }
    u ()
    """
  }

  void testPromo2() {
    shell.evaluate """
    @Typed
    def u () {
      def C = '1'.toCharacter()
      def BD = new BigDecimal("1.0")
      assert (C - BD instanceof BigDecimal)
    }
    u ()
    """
  }


  void testPromo3() {
    shell.evaluate """
    @Typed
    def u () {
      def C = '1'.toCharacter()
      def B = new Byte("1")
      def BD = new BigDecimal("1.0")

      assert (C + B instanceof Integer)
      assert (C - BD instanceof BigDecimal)
    }
    u ()
    """
  }
}