/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
