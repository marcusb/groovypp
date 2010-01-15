@Typed package groovy.util.concurrent

import groovy.util.concurrent.AtomicMapEntry
import java.util.concurrent.atomic.AtomicLong

class AtomicLongMap<K> extends AtomicMap<K,AtomicLongMap.Entry<K>> {

    static class Entry<K> extends AtomicLong implements AtomicMapEntry<K,AtomicLong> {}

    Entry<K> createEntry(K key, int hash) {
        [key:key, hash:hash]
    }
}