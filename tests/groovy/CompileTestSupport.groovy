package groovy

import org.codehaus.groovy.control.CompilationFailedException
import static org.junit.Assert.fail
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.syntax.SyntaxException;

public class CompileTestSupport {
  static void shouldNotCompile(String script, String messageFragment = "") {
    GroovyShell shell = new GroovyShell();
    try {
      shell.evaluate (script)
    } catch (MultipleCompilationErrorsException e) {
      assert e.message.contains(messageFragment)
      println e.errorCollector.errors[0].class 
      def error = e.errorCollector.errors[0].cause
      if (error instanceof SyntaxException) {
        assert error.startLine > 0 && error.startColumn > 0
      } else {
        assert error.line > 0 && error.column > 0
      }
      return;
    } catch (CompilationFailedException e) {
      fail("Unknown error occured while compiling");
    }
    fail("Given code fragment should not compile");
  }

  static void shouldCompile(String script) {
    GroovyShell shell = new GroovyShell();
    try {
      shell.evaluate (script)
    } catch (CompilationFailedException e) {
      fail("Given code fragment should compile");
    }
  }

}