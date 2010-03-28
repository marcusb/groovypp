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