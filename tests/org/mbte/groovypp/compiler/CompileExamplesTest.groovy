package org.mbte.groovypp.compiler

import org.codehaus.groovy.tools.FileSystemCompiler
import org.codehaus.groovy.control.CompilerConfiguration
import groovy.util.test.GroovyFileSystemCompilerTestCase

public class CompileExamplesTest extends GroovyFileSystemCompilerTestCase {
    void testCompile () {
        def finder = new FileNameFinder ()
        
        String [] names = finder.getFileNames("./Examples/src/", "**/*.groovy")
        names.each {
            println it
        }

        compiler.compile (names)
    }
}