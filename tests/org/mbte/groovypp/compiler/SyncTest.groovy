package org.mbte.groovypp.compiler

public class SyncTest extends GroovyShellTestCase {
  void testSync () {
    shell.evaluate """
    @Compile
    def u () {
      def o = new Object ();
      def exception = false
      try {
        synchronized (o) {
           assert !Thread.currentThread ().holdsLock(o)
        }
      }
      catch (Throwable t) {
           exception = true
           assert !Thread.currentThread ().holdsLock(o)
      }
      assert exception && !Thread.currentThread ().holdsLock(o)
    }
    u ()
    """
  }
}