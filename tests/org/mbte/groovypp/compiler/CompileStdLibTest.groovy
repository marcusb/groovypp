package org.mbte.groovypp.compiler

import org.codehaus.groovy.tools.FileSystemCompiler
import org.codehaus.groovy.control.CompilerConfiguration
import groovy.util.test.GroovyFileSystemCompilerTestCase

class CompileStdLibTest extends GroovyFileSystemCompilerTestCase {
    void testCompile () {
        def finder = new FileNameFinder ()
        
        def names = finder.getFileNames("./StdLib/src/", "**/*.groovy")
        names.addAll(finder.getFileNames("./StdLib/src/", "**/*.java"))
        names.each {
            println it
        }
        compiler.compile (names as String[])
    }
}