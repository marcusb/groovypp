package org.mbte.groovypp.compiler

import org.codehaus.groovy.runtime.InvokerInvocationException
import org.codehaus.groovy.runtime.InvokerHelper;

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

  void testIfaceSetter () {
    def res = shell.evaluate ("""
interface I {
  void setValue (def x)
}

@Compile
class CI implements I {
  def v

  void setValue(def x) { v = x }

  def test (def newV) {
    I ci = new CI ()
    ci.value = newV
    ((CI)ci).v
  }
}

new CI ()
    """)
    assertEquals (10, InvokerHelper.invokeMethod(res, "test", 10))
  }
}

