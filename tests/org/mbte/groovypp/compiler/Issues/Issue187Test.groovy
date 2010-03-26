package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldCompile
import static groovy.CompileTestSupport.shouldNotCompile

public class Issue187Test extends GroovyShellTestCase {
  void testStaticInner() {
        shell.evaluate """
@Typed
class ConstructorTest {
	static void main(String[] args) {
        def v = 3
		def a = new A(2){{this.par = v}}
        assert a.par == 3
	}

	static class A {
	   int par	
       A(int par=0) {this.par=par}
	}
}        """
    }

    void testNonStaticInner() {
          shouldNotCompile ("""
  @Typed
  class ConstructorTest {
      static void main(String[] args) {
          new B(){}
      }

      class B {
      }
  }        """, "Can not instantiate non-static inner class ConstructorTest\$B in static context")
      }
}

