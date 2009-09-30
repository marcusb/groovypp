package groovy

import org.codehaus.groovy.control.CompilationFailedException
import static org.junit.Assert.fail;

public class CompileTestSupport {
  static void shouldNotCompile(String script) {
    GroovyShell shell = new GroovyShell();
    try {
      shell.evaluate (script)
    } catch (CompilationFailedException e) {
      return;
    }
    fail("Given code fragmet should not compile");
  }

  static void shouldCompile(String script) {
    GroovyShell shell = new GroovyShell();
    try {
      shell.evaluate (script)
    } catch (CompilationFailedException e) {
      fail("Given code fragmet should compile");
    }
  }

}