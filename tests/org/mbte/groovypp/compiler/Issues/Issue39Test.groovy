package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue39Test extends GroovyShellTestCase {
    void testCompile () {
        shell.evaluate """
        @Typed def u () {
            Iterable<Pair<Integer,Long>> arr = new LinkedList<Pair<Integer,Long>> ()
            arr.asList().sort{ a, b -> a.first <=> b.first }
        }
        """
    }
}