package groovy

class ModuloTest extends GroovyShellTestCase {


  void testModuloLesser() {
      shell.evaluate  """

      @Typed
      def u() {
        int modulo = 100
        for (i in 0..modulo-1) {
          assert (i%modulo)==i
        }
      }

      u()
    """
  }

  void testModuloEqual() {
    shell.evaluate  """

    @Typed
    def u() {
      int modulo = 100
      for (i in 0..modulo) {
        assert ((i*modulo) % modulo)==0
      }
    }

    u()
    """

  }

  void testModuloBigger() {
    shell.evaluate  """

    @Typed
    def u() {
      int modulo = 100
      for (i in 0..modulo-1) {
        assert ((i*modulo+i) % modulo)==i
      }
    }

    u()
    """

  }

}
