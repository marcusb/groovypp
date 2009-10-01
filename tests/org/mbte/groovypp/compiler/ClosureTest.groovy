package org.mbte.groovypp.compiler

public class ClosureTest extends GroovyShellTestCase {

  void testClosure() {
    def res = shell.evaluate("""
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Typed
class MailBox {
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    protected final void loop (TypedClosure<MailBox> operation) {
        executor.submit {
            operation.setDelegate this
            operation ()
            loop (operation)
        }
    }
}

1
""")
  }

  void testClosureAsObj() {
    def res = shell.evaluate("""
    import java.util.concurrent.ExecutorService
    import java.util.concurrent.Executors

    @Typed
    class MailBox {
        private static final ExecutorService executor = Executors.newCachedThreadPool();

        protected final void loop (TypedClosure<MailBox> operation) {
            executor.submit {
                operation.setDelegate this
                operation ()
                loop (operation)
            }
        }
    }

    1
    """)
  }
}