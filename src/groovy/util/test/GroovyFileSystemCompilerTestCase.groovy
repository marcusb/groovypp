package groovy.util.test

import org.codehaus.groovy.tools.FileSystemCompiler
import org.codehaus.groovy.control.CompilerConfiguration

@Typed abstract class GroovyFileSystemCompilerTestCase extends GroovyTestCase {

    protected FileSystemCompiler compiler
    protected File outputDir, stubDir

    protected void setUp() {
        super.setUp()

        outputDir = File.createTempFile("gfsctc", null)
        if (!outputDir.delete()) throw new IllegalStateException("Could not delete temp file")
        outputDir.mkdir()

        stubDir = File.createTempFile("gfsctc", null)
        if (!stubDir.delete()) throw new IllegalStateException("Could not delete temp file")
        stubDir.mkdir()

        def compilerConfiguration = new CompilerConfiguration()
        compilerConfiguration.setTargetDirectory(outputDir)
        compilerConfiguration.setJointCompilationOptions(["stubDir":stubDir]) 
        compiler = new FileSystemCompiler (compilerConfiguration)
    }

    protected void tearDown() {
        compiler = null
        outputDir.deleteDir()
        stubDir.deleteDir()
        super.tearDown()
    } 
}
