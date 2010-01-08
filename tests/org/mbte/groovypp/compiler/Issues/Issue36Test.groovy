package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue36Test extends GroovyShellTestCase {
    void testEachWithIndex () {
        shell.evaluate """
@Typed
class A
{
    def property
    public static void main( String ... args )
    {
        B b = new B();
        report( 'aaaa', B.top( 10, b.getMap()));
    }


    static void report( String title, Map<String, Long> map )
    {
    }

}

class B
{
    def Map<String, Long> map = new HashMap<String, Long>();


    static Map<String, Long> top ( int n, Map<String, Long> map )
    {
        return null;
    }
}        """
    }
}