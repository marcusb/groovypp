package org.mbte.groovypp.compiler.Issues

public class Issue15Test extends GroovyShellTestCase {
    void testNpe () {
        shell.evaluate """
package widefinder

@Typed
class Start
{
    static final INT     LINES     = new INT();

    static class INT
    {
        int  counter = 0;
        void increment(){ this.counter++ }
        void decrement(){ --this.counter }
    }

    public static void main ( String[] args )
    {
       def i = new INT ()
       (0..10).each {
          assert  1 == ++i.counter
          assert  0 == --i.counter
          assert  0 ==   i.counter--
          assert -1 ==   i.counter++

          i.counter = 0
          i.increment ()
          i.decrement ()
       }
       assert i.counter == 0
    }
}        """
    }
}