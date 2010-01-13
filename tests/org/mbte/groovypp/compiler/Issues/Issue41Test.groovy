package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

public class Issue41Test extends GroovyShellTestCase {
    void testBug () {
    shell.evaluate( """
      @Typed class C {
        int f
        int getF() {this.f}
      }
      new C().getF()
      """)
  }
}