package groovy.util.concurrent

import org.codehaus.groovy.util.AbstractConcurrentMap

@Trait abstract class AtomicMapEntry<K,V> implements AbstractConcurrentMap.Entry<K,V> {
    K key

    V getValue () { this }

    int hash

    boolean isEqual(K key, int hash) {
        this.hash == hash && this.key == key
    }

    void setValue(V value) {}

    boolean isValid() { true }
}