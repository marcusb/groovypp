@Typed package groovy.util.concurrent

import groovy.util.concurrent.AtomicMapEntry
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class AtomicReferenceMap<K,V> extends AtomicMap<K,AtomicReferenceMap.Entry<K,V>> {

    static class Entry<K,V> extends AtomicReference<V> implements AtomicMapEntry<K,AtomicReference<V>> {}

    Entry<K,V> createEntry(K key, int hash) {
        [key:key, hash:hash]
    }
}