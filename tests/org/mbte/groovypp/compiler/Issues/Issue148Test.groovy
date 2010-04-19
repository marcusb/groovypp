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

package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue148Test extends GroovyShellTestCase {
  void testMe () {
        shell.evaluate """
@Typed class Test{
    static main(args) {
       Helper h = new Helper()
       [1].each(h.&foo)
    }
}

class Helper {
    def foo(x) {}
}
        """
    }

  void testReceiverEval () {
        shell.evaluate """
@Typed class Test{
    static main(args) {
       [1, 2, 3].each(new Helper().&foo)
       assert 1 == Helper.instancesCreated
       assert 3 == Helper.methodCalled
    }
}

class Helper {
    static int instancesCreated, methodCalled
    Helper() {
        instancesCreated++
    }

    def foo(x) {
      methodCalled++
    }
}
        """
    }
}