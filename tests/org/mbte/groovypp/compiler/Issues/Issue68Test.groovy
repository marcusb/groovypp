package org.mbte.groovypp.compiler.Issues

import org.mbte.groovypp.ReleaseInfo

public class Issue68Test extends GroovyTestCase {
    void testGroovyPPVersion () {
        def tokens = ReleaseInfo.version.tokenize(".")

        assert tokens.size() == 3
        Integer.parseInt(tokens[0])
        Integer.parseInt(tokens[1])
        Integer.parseInt(tokens[2])
    }
}
