package groovy

import org.codehaus.groovy.control.CompilationFailedException
import static org.junit.Assert.fail
import org.codehaus.groovy.control.MultipleCompilationErrorsException;

public class CompileTestSupport {
  static void shouldNotCompile(String script, String messageFragment = "") {
    GroovyShell shell = new GroovyShell();
    try {
      shell.evaluate (script)
    } catch (MultipleCompilationErrorsException e) {
      assert e.message.contains(messageFragment)
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