package org.mbte.groovypp.compiler

import static groovy.CompileTestSupport.shouldNotCompile
/**
 * @author ven
 */
class AccessTest extends GroovyShellTestCase {
  void testPrivateMethod() {
    shouldNotCompile """
      @Typed
      void foo(Class c) {
        c.initAnnotationsIfNecessary()
      }
    """
  }

  void testPrivateField() {
    shouldNotCompile """
      @Typed
      void foo(Class c) {
        c.publicFields
      }
    """
  }
}
