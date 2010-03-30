package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldCompile

@Typed
class Issue50Test extends GroovyShellTestCase {

    void testMapEntry()
    {
        shouldCompile """
@Typed package p

import java.util.concurrent.*

class Start
{
    def bar ()
    {
        ExecutorService pool = null

        pool.submit(( Runnable ) [ run : { 1;
            println "fishy print"
        }])
    }
}
        """
    }

    void testMapEntryList()
    {
        shouldCompile """
@Typed package p

import java.util.concurrent.*

class Start
{
    def bar ()
    {
        ExecutorService pool = null

        pool.submit(( Runnable ) [ run : { 1;
            println "fishy print"
        }, cool:true])
    }
}
        """
    }
}