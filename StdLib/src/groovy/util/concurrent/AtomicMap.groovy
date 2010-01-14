@Typed package groovy.util.concurrent

import org.codehaus.groovy.util.AbstractConcurrentMap
import org.codehaus.groovy.util.AbstractConcurrentMapBase
import groovy.util.concurrent.AtomicMapEntry

abstract class AtomicMap<K,V> implements Iterable<AtomicMapEntry<K,V>> {

    private final Map<K,V> map = new Map ()

    protected static <K> int hash(K key) {
        def h = key.hashCode ()
        h += ~(h << 9)
        h ^=  (h >>> 14)
        h +=  (h << 4)
        h ^=  (h >>> 10)
        h
    }

    V getAt(K key) {
        def h = hash(key)
        map.segmentFor(h).getOrPut(key, h, null).value
    }

    void remove (K key) {
        def h = hash(key)
        map.segmentFor(h).remove(key, h)
    }

    Iterator<AtomicMapEntry<K,V>> iterator () {
        map.iterator()
    }

    abstract AtomicMapEntry<K,V> createEntry(K key, int hash)

    private class Map<K,V> extends AbstractConcurrentMap<K,V> implements Iterable<AtomicMapEntry<K,V>> {

        Map() { super(null) }

        protected AbstractConcurrentMapBase.Segment createSegment(Object segmentInfo, int cap) {
            return new Segment(cap)
        }

        Iterator<AtomicIntegerMap.Entry> iterator () {
            segments.iterator().map { s -> ((Segment)s).iterator() }.flatten ()
        }

        private class Segment<K,V> extends AbstractConcurrentMap.Segment<K,V> implements Iterable<AtomicMapEntry<K,V>> {
            Segment(int cap) {
                super(cap);
            }

            protected AbstractConcurrentMap.Entry<K> createEntry(K key, int hash, V unused) {
                createEntry(key, hash)
            }

            Iterator<AtomicMapEntry<K,V>> iterator () {
                new MyIterator<K> (table)
            }
        }
    }

    private static class MyIterator<K> implements Iterator<AtomicMapEntry<K>> {
        final Object [] table
        int index = 0, innerIndex = 0

        MyIterator (Object [] t) {
            this.@table = t
        }

        boolean hasNext() {
            while (index < table.length) {
                def o = table[index]
                if (!o) {
                    index++
                    continue
                }

                return true
            }

            false
        }

        AtomicMapEntry<K,V> next() {
            def o = table[index]
            if (o instanceof AbstractConcurrentMap.Entry) {
                index++
                o
            }
            else {
                def arr = (Object[])o
                def res = arr [innerIndex++]
                if (innerIndex == arr.length) {
                    innerIndex = 0
                    index++
                }
                res
            }
        }

        void remove() {
            throw new UnsupportedOperationException()
        }
    }
}