package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

public class Issue153Test extends GroovyShellTestCase {
    void testNotOKSetterReturningObject() {
        shouldNotCompile("""
                @Typed package test
                
                class Test{
                    static main(args) {
                        def c = new Helper()
                        c.prop = 'x'
                    }
                }
                
                class Helper {
                    public setProp(String s) { }
                }
            """,
           'Cannot find property prop')
    }

  void testOKSetterReturningVoid() {
        shell.evaluate """
            @Typed package test
            
            class Test{
                static main(args) {
                    def c = new Helper()
                    c.prop = 'x'
                }
            }
            
            class Helper {
                public void setProp(String s) { }
            }
        """
    }
}