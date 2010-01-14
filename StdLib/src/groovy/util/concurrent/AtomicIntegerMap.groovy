@Typed package groovy.util.concurrent

import org.codehaus.groovy.util.AbstractConcurrentMap
import java.util.concurrent.atomic.AtomicInteger
import org.codehaus.groovy.util.AbstractConcurrentMapBase
import groovy.util.concurrent.AtomicMapEntry

class AtomicIntegerMap<K> extends AtomicMap<K,AtomicIntegerMap.Entry<K>> {

    static class Entry<K> extends AtomicInteger implements AtomicMapEntry<K,AtomicInteger> {}

    Entry<K> createEntry(K key, int hash) {
        [key:key, hash:hash]
    }
}