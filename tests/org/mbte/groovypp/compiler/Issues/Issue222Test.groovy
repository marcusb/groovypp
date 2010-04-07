package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldCompile

@Typed
public class Issue222Test extends GroovyShellTestCase {
    void testMe () {
        shouldCompile """
import java.util.concurrent.Executor

@Typed
class CLP {
  static Executor getOrElse (Closure factory) {

  }

  def f(int concurrencyLevel = Integer.MAX_VALUE,
        Executor executor = CLP.getOrElse { return  null }) {

  }

}        """
    }

    void testMe2 () {
        shouldCompile """
import java.util.concurrent.Executor

@Typed
class CLP {
  static Executor getOrElse (Closure factory) {

  }

  def f(int concurrencyLevel = Integer.MAX_VALUE,
        Executor executor = {}) {

  }

}        """
    }
}