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

package org.mbte.groovypp.compiler.Issues

class Issue196Test extends GroovyTestCase {

	private File dynSrcDir
	private File scriptOne
    private File scriptTwo

    private List allFiles = [scriptOne, scriptTwo]

	public void setUp(){
        super.setUp()
        locateCompilerTestDir()
		
		scriptOne = new File(dynSrcDir, 'Issue196One.gpp')
        scriptOne.delete()
		scriptOne << """
            class Issue196One {
                static main(args) {
                    Issue196Two two = new Issue196Two()
                    throw new RuntimeException('Issue196One evaluated successfully')
                }
            }
		"""
		
        scriptTwo = new File(dynSrcDir, 'Issue196Two.gpp')
        scriptTwo.delete()
        scriptTwo << """
            class Issue196Two {}
        """
    }
	
	public void tearDown(){
	    try {
            super.tearDown()
            allFiles*.delete()
 		} catch(Exception ex){
 			throw new RuntimeException("Could not delete source files dynamically created in " + dynSrcDir, ex)
 		}
	}

    void testGPPFilesBeingResolvedToScript() {
        GroovyShell shell = new GroovyShell(this.class.classLoader)
        try {
            shell.evaluate(scriptOne)
            fail('If script Issue196One was evaluated successfully, it should have thrown RuntimeException.')
        } catch(RuntimeException ex) {
            assert ex.message.contains('Issue196One evaluated successfully')
        }  
    }
    
	private void locateCompilerTestDir(){
		String bogusFile = "bogusFile"
	   	File f = new File(bogusFile)
	   	String path = f.getAbsolutePath()
	   	path = path.substring(0, path.length() - bogusFile.length())
	   	dynSrcDir = new File(path + File.separatorChar + 'out' + File.separatorChar + 'test' + File.separatorChar + 'Groovypp')
	}

}