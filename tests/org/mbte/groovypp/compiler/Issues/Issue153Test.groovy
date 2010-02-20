package org.mbte.groovypp.compiler.Issues

import org.codehaus.groovy.control.MultipleCompilationErrorsException

public class Issue153Test extends GroovyShellTestCase {
    void testNotOKSetterReturningObject() {
        try {
            shell.evaluate """
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
            """
            fail('Compilation should have failed as setter setProp() does not return void')
          } catch (MultipleCompilationErrorsException e) {
              def error = e.errorCollector.errors[0].cause
              assertTrue error.message.contains('Cannot find property prop') 
          }
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