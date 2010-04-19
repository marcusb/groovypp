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

import org.mbte.groovypp.compiler.DebugContext

public class Issue175Test extends GroovyShellTestCase {
  void testMe() {
        def baos = new ByteArrayOutputStream()
        def oldStream = DebugContext.outputStream
        try {
          DebugContext.outputStream = new PrintStream(baos)
          shell.evaluate """
            @Typed(debug=true)  // We check for debug output below!!!
            static def foo() {
              print "foo"
            }
            foo()
          """
        } finally {
          DebugContext.outputStream = oldStream
        }

        assert baos.toString().indexOf("forName") < 0
    }
}