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