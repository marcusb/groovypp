@Typed package groovy.util.concurrent

import org.codehaus.groovy.util.AbstractConcurrentMap
import java.util.concurrent.atomic.AtomicInteger
import org.codehaus.groovy.util.AbstractConcurrentMapBase

class AtomicIntegerMap<K> implements Iterable<AtomicIntegerMap.Entry<K>> {

    private final Map<K> map = []

    protected static <K> int hash(K key) {
        def h = key.hashCode ()
        h += ~(h << 9)
        h ^=  (h >>> 14)
        h +=  (h << 4)
        h ^=  (h >>> 10)
        h
    }

    AtomicInteger getAt(K key) {
        def h = hash(key)
        map.segmentFor(h).getOrPut(key, h, null)
    }

    void remove (K key) {
        def h = hash(key)
        map.segmentFor(h).remove(key, h)
    }

    Iterator<Entry<K>> iterator () {
        map.iterator()
    }

    static class Entry<K> extends AtomicInteger {
        K key
    }

    private static class Map<K> extends AbstractConcurrentMap<K,AtomicInteger> implements Iterable<AtomicIntegerMap.Entry<K>> {

        Map() { super(null) }

        protected AbstractConcurrentMapBase.Segment createSegment(Object segmentInfo, int cap) {
            return new Segment(cap)
        }

        Iterator<AtomicIntegerMap.Entry> iterator () {
            segments.iterator().map { s -> ((Segment)s).iterator() }.flatten ()
        }

        private static class Segment<K> extends AbstractConcurrentMap.Segment<K,AtomicInteger> implements Iterable<AtomicIntegerMap.Entry<K>> {
            Segment(int cap) {
                super(cap);
            }

            protected AtomicIntegerMap.Entry<K> createEntry(K key, int hash, AtomicInteger unused) {
                new Entry(hash:hash, key:key)
            }

            protected AbstractConcurrentMap.Entry<K> createEntry(Object key, int hash, Object unused) {
                new Entry(hash:hash, key:key)
            }

            Iterator<AtomicIntegerMap.Entry<K>> iterator () {
                new MyIterator<K> (table)
            }

            private static class Entry extends AtomicIntegerMap.Entry implements AbstractConcurrentMap.Entry<K,AtomicInteger> {
                int hash

                boolean isEqual(K key, int hash) {
                    this.@hash == hash && this.key == key
                }

                AtomicInteger getValue() { this }

                void setValue(AtomicInteger value) {}

                void setValue(Object value) {}

                boolean isValid() { true }
            }

            private static class MyIterator<K> implements Iterator<AtomicIntegerMap.Entry<K>> {
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

                AtomicIntegerMap.Entry<K> next() {
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
    }
}