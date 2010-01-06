package org.mbte.groovypp.compiler

import org.codehaus.groovy.tools.FileSystemCompiler
import org.codehaus.groovy.control.CompilerConfiguration


class GroovyFileSystemCompilerTestCase extends GroovyTestCase {

    protected FileSystemCompiler compiler;
    protected File outputDir;

    protected void setUp() {
        super.setUp()

        outputDir = new File("tmp" + Math.random())
        outputDir.mkdir()

        def compilerConfiguration = new CompilerConfiguration()
        compilerConfiguration.setTargetDirectory(outputDir)
        compiler = new FileSystemCompiler (compilerConfiguration)
    }

    protected void tearDown() {
        compiler = null
        outputDir.deleteDir()
        super.tearDown()
    } 
}
