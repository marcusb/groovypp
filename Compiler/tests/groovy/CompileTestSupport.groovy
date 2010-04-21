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

package groovy

import org.codehaus.groovy.control.CompilationFailedException
import static org.junit.Assert.fail
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.syntax.SyntaxException

public class CompileTestSupport {
  static void shouldNotCompile(String script, String messageFragment = "") {
    try {
      new GroovyShell().parse(script)
    } catch (MultipleCompilationErrorsException e) {
      assert e.message.contains(messageFragment)
      def error = e.errorCollector.errors[0].cause
      if (error instanceof SyntaxException) {
        assert error.startLine > 0 && error.startColumn > 0
      } else {
        assert error.line > 0 && error.column > 0
      }
      return
    } catch (CompilationFailedException e) {
      fail("Unknown error occured while compiling")
    }
    fail("Given code fragment should not compile")
  }

  static void shouldCompile(String script) {
    try {
      new GroovyShell().parse(script)
    } catch (CompilationFailedException e) {
      fail("Given code fragment should compile")
    }
  }

}