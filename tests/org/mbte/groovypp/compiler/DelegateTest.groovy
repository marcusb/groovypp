package org.mbte.groovypp.compiler;

public class DelegateTest extends GroovyShellTestCase { 
  void testDelegate () {
    shell.evaluate """
public interface Pool {
    void resize(int poolSize);

    void resetDefaultSize();

    void execute(Runnable task);

    void shutdown();
}
       @Compile
       abstract class Group {
          protected @Delegate Pool pool
       }

0
    """
  }


  void testField () {
    def res = shell.evaluate ("""
@Compile
class CommonActorImpl  {
    protected volatile boolean sendRepliesFlag = true

    protected final boolean getSendRepliesFlag() {sendRepliesFlag}

    protected final void enableSendingReplies() { sendRepliesFlag = true }

    protected final void disableSendingReplies() { sendRepliesFlag = false }
}

new CommonActorImpl ()
    """)

    res.sendRepliesFlag = false
    assertFalse res.sendRepliesFlag
    res.sendRepliesFlag = true
    assertTrue  res.sendRepliesFlag
  }
}