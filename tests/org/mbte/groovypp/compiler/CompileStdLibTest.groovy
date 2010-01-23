package org.mbte.groovypp.compiler

import org.codehaus.groovy.tools.FileSystemCompiler
import org.codehaus.groovy.control.CompilerConfiguration
import groovy.util.test.GroovyFileSystemCompilerTestCase

class CompileStdLibTest extends GroovyFileSystemCompilerTestCase {
    void testCompile () {
        def finder = new FileNameFinder ()
        
        String [] names = finder.getFileNames("./StdLib/src/", "**/*.groovy")
        names.each {
            println it
        }
        compiler.compile (names)
    }
}