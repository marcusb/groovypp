package org.mbte.groovypp.compiler

import org.codehaus.groovy.tools.FileSystemCompiler
import org.codehaus.groovy.control.CompilerConfiguration

public class CompileStdLibTest extends GroovyTestCase {
    void testCompile () {
        def finder = new FileNameFinder ()
//        String [] names = finder.getFileNames("./StdLib/src/", "**/*.groovy")
//        names.each {
//            println it
//        }
//        new FileSystemCompiler (new CompilerConfiguration()).compile (names)

        String [] names = finder.getFileNames("./StdLibTest/test/", "**/*.groovy")
        names.each {
            println it
        }
        new FileSystemCompiler (new CompilerConfiguration()).compile (names)
    }
}