package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

@Typed
class Issue82Test extends GroovyShellTestCase {

    void testCyclicUsageCompilationError_WithTyped()
    {
        shouldNotCompile """
          @Typed
          class SiteVisitor implements SiteVisitor {}
        """
    }

    void testCyclicUsageCompilationError_WithoutTyped()
    {
        shouldNotCompile """
          class SiteVisitor implements SiteVisitor {}
        """
    }
}