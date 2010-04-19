/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.groovypp.compiler

import org.codehaus.groovy.runtime.InvokerHelper

public class DelegateTest extends GroovyShellTestCase {
  void testDelegate() {
    shell.evaluate """
public interface Pool {
    void resize(int poolSize);

    void resetDefaultSize();

    void execute(Runnable task);

    void shutdown();
}
       @Typed
       abstract class Group {
          protected @Delegate Pool pool
       }

0
    """
  }


  void testField() {
    def res = shell.evaluate("""
@Typed
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
    assertTrue res.sendRepliesFlag
  }

  void testIfaceSetter() {
    def res = shell.evaluate("""
interface I {
  void setValue (def x)
}

@Typed
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
    assertEquals(10, InvokerHelper.invokeMethod(res, "test", 10))
  }
}

