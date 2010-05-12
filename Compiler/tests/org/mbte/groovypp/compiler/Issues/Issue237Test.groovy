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

public class Issue237Test extends GroovyShellTestCase {
    void testBug () {
	    def res = shell.evaluate("""
	      @Typed
	      public def foo() {
	        def res = []
	        "aa\\nab\\nac".eachLine { line->
				assert line.startsWith("a")
				res << line
			}
	        res
	      }
	      foo()
	    """)
	    assertEquals(["aa", "ab", "ac"], res)	    
    }

	void test2pars () {
		def res = shell.evaluate("""
		  @Typed
		  public def foo() {
		    def res = []
		    def sum = 0
		    "aa\\nab\\nac".eachLine { line, index ->
				assert line.startsWith("a")
				res << index
			}
		    res
		  }
		  foo()
		""")
		assertEquals([0, 1, 2], res)
	}

	void testSplitEachLine() {
		def res = shell.evaluate("""
		  @Typed
		  public def foo() {
		    def res = []
		    def sum = 0
		    "aa ab\\nac ad\\nae af".splitEachLine(" ") { words ->
				for (String s : words) {
					res << s
				}
			}
		    res
		  }
		  foo()
		""")
		assertEquals(["aa", "ab", "ac", "ad", "ae", "af"], res)
	}

}