package org.mbte.groovypp.compiler

import static groovy.CompileTestSupport.shouldNotCompile
/**
 * @author ven
 */
class AccessTest extends GroovyShellTestCase {
  void testPrivate() {
    shouldNotCompile """
      @Typed
      void foo(Class c) {
        c.initAnnotationsIfNecessary()
      }
    """
  }
}
