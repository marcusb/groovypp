package org.mbte.groovypp.compiler

public class SyncTest extends GroovyShellTestCase {
  void testSync () {
    shell.evaluate """
    @Compile
    def u () {
      def o = new Object ();
      try {
        synchronized (o) {
           assert !Thread.currentThread ().holdsLock(o)
        }
      }
      catch (Throwable t) {
           assert !Thread.currentThread ().holdsLock(o)
      }
      assert !Thread.currentThread ().holdsLock(o)
    }
    u ()
    """
  }
}