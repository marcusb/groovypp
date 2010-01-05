package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldCompile

public class Issue35Test extends GroovyShellTestCase {
    void testBug () {
        shouldCompile("""
            @Typed
            def foo () {
               Map<Long, Collection<String>> topCountersMap = new HashMap<Long, Collection<String>>( 1 )
            }
            foo()
        """)
    }
}