package groovy

class StringBufferTest extends GroovyShellTestCase {
    void testSize() {
      shell.evaluate  """

        @Typed
        def u() {
          def x = new StringBuffer()
          assert x.size() == x.length()
          x = new StringBuffer('some text')
          assert x.size() == x.length()
        }

        u()
      """

    }

  /*
    void testPutAt(){
        shell.evaluate  """

          @Typed
          def u() {
            def buf = new StringBuffer('0123')

            buf[1..2] = 'xx'
            assert '0xx3' == buf.toString()
            buf = new StringBuffer('0123')
            buf[1..2] = 99
            assert '0993' == buf.toString()
            buf = new StringBuffer('0123')
            buf[0..<0] = 'xx'
            assert 'xx0123' == buf.toString(), 'border case left'
            buf = new StringBuffer('0123')
            buf[4..4] = 'xx'
            println buf.toString()
            assert '0123xx' == buf.toString(), 'border case right'
            // more weird Ranges already tested in ListTest
          }

          u()
        """

    }
    */
}