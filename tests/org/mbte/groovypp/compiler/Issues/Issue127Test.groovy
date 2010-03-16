package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue127Test extends GroovyShellTestCase {
    void testPostfix () {
       shell.evaluate """
         @Typed class Test {
           static main(args) {
              Reference count = [0]
              def l = [10,11,12,13,14,15]
              l.eachWithIndex {
                int n, int idx -> assert count++ == idx
              }
              assert count == l.size() 
           }
         }
       """
    }

    void testPrefix () {
       shell.evaluate """
         @Typed class Test {
           static main(args) {
              Reference count = [0]
              def l = [10,11,12,13,14,15]
              l.eachWithIndex {
                int n, int idx -> assert ++count == idx + 1
              }
              assert count == l.size() 
           }
         }
       """
    }
}