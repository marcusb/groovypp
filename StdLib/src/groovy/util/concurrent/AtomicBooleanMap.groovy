@Typed package groovy.util.concurrent

import groovy.util.concurrent.AtomicMapEntry
import java.util.concurrent.atomic.AtomicBoolean

class AtomicBooleanMap<K> extends AtomicMap<K,AtomicBooleanMap.Entry<K>> {

    static class Entry<K> extends AtomicBoolean implements AtomicMapEntry<K,AtomicBoolean> {}

    Entry<K> createEntry(K key, int hash) {
        [key:key, hash:hash]
    }
}