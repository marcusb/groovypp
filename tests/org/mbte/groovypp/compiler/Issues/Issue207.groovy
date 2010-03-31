package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldCompile

public class Issue207Test extends GroovyShellTestCase {
    void testMe() {
      shouldCompile("""
@Typed
package widefinder.maps 

import org.junit.Test


class MyTest extends GroovyTestCase
{
    @Test
    void testSize()
    {
        MyMap m = new MyMap<Object, Integer>( 10000 )
        1.upto( 100000 ){ int j -> m.put( j, j ) }

        1.upto( 100000 ){ int j -> m.put( j, j ) }

        1.upto( 100000 ){ int j -> m.put( "[\$j]", j ) }

        1.upto( 100000 ){ int j -> m.put( "[\$j][new]", j ) }
    }
}



public class MyMap<K, V> implements Map<K, V>
{
    public MyMap ( int capacity )    {    }

    public boolean isEmpty ()    {        true    }

    public boolean containsKey ( Object key ){false}

    public boolean containsValue ( Object value ){false}

    public void clear (){}

    public V get ( Object key ){null}

    public V put ( K key, V value ) { null }

    public V remove ( Object key ) { null }

    public void putAll ( Map<? extends K, ? extends V> m ) {}

    public int size () { null }

    public Set<K> keySet () { null }

    public Collection<V> values () { null }

    public Set<Map.Entry<K, V>> entrySet () { null }
}""")
    }
}