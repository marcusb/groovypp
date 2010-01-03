package org.mbte.groovypp.compiler.Issues

public class Issue31Test extends GroovyShellTestCase {
    void testBug () {
        shell.evaluate """
            @Typed
            void testFoldLeftWithMap () {
                def res = [1,2,3,3,2,1].foldLeft (new HashMap<?,Integer>()) { el, map ->
                    map[el] = map.get(el,0) + 1
                }
                assertEquals res, [1:2, 2:2, 3:2]
            }
        """
    }
}