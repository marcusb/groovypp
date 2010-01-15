package org.mbte.groovypp.compiler.Issues

public class Issue45Test extends GroovyShellTestCase {
    void testInner () {
        shell.evaluate """
import groovy.util.concurrent.*
import org.codehaus.groovy.util.AbstractConcurrentMap
import org.codehaus.groovy.util.AbstractConcurrentMapBase

@Typed class A {
    def m = new Map ()

    private class Map<K,V> extends AbstractConcurrentMap<K,V> implements Iterable<AtomicMapEntry<K,V>> {
        Map() { super(null) }

        protected AbstractConcurrentMapBase.Segment createSegment(Object segmentInfo, int cap) {}

        Iterator iterator () {}
    }
}

new A ()
"""
    }

    void testMe () {
        shell.evaluate """
            @Typed package p

            import groovy.util.concurrent.*

            def m = new AtomicIntegerMap<String> ()
            m ['10'].incrementAndGet ()
        """
    }
}