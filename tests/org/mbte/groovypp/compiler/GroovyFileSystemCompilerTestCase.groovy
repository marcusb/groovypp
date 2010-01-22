package org.mbte.groovypp.compiler

import org.codehaus.groovy.tools.FileSystemCompiler
import org.codehaus.groovy.control.CompilerConfiguration


@Typed abstract class GroovyFileSystemCompilerTestCase extends GroovyTestCase {

    protected FileSystemCompiler compiler;
    protected File outputDir;

    protected void setUp() {
        super.setUp()

        outputDir = File.createTempFile("gfsctc", null)
        if (!outputDir.delete()) throw new IllegalStateException("Could not delete temp file")
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
