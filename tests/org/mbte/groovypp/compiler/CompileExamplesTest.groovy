package org.mbte.groovypp.compiler

import org.codehaus.groovy.tools.FileSystemCompiler
import org.codehaus.groovy.control.CompilerConfiguration

public class CompileExamplesTest extends GroovyTestCase {
    void testCompile () {
        def finder = new FileNameFinder ()
        
        String [] names = finder.getFileNames("./Examples/src/", "**/*.groovy")
        names.each {
            println it
        }
        new FileSystemCompiler (new CompilerConfiguration()).compile (names)
    }
}