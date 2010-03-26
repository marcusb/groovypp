package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

public class Issue199Test extends GroovyShellTestCase {
  void testMe() {
        shouldNotCompile("""
          @Typed package p
          throw ""
         """,
  'Only java.lang.Throwable objects may be thrown')
    }

  void testDuckTypedException() {
        shell.evaluate("""
          @Typed package p
          try{} catch(ex){throw ex}
         """)
  }
}