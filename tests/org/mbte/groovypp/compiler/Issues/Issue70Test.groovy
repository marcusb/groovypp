package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue70Test extends GroovyShellTestCase {
    void testBug () {
        shell.evaluate """
import org.codehaus.groovy.runtime.DefaultGroovyMethods

@Typed
class Test {
    static {
        println "<clinit> called"
    }

    static main(args) {}

    static <T> T each(T self, Closure closure) {
        DefaultGroovyMethods.each(self, closure)
    }
}
        """
    }
}