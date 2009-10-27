package groovy.util;

import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport;

/**
 * Closure for filtering
 */
@Typed
abstract class FilterMapClosure<K,V> extends DefaultGroovyMethodsSupport {
    abstract boolean check(K key, V value);

    static <K,V> Map.Entry<K,V> find(Map<K,V> self, FilterMapClosure<K,V> condition) {
        for (Map.Entry<K, V> entry : self.entrySet()) {
            if (condition.check(entry.getKey(), entry.getValue()))
                return entry
        }
        return null
    }

    static <K,V> Map<K,V> findAll(Map<K,V> self, FilterMapClosure<K,V> condition) {
        Map<K, V> map = createSimilarMap(self)
        for (Map.Entry<K, V> entry : self.entrySet()) {
            if (condition.check(entry.getKey(), entry.getValue()))
                map.put(entry.getKey(), entry.getValue())
        }
        map
    }

    static <K,V> boolean any(Map<K,V> self, FilterMapClosure<K,V> condition) {
        for (Map.Entry<K, V> entry : self.entrySet()) {
            if (condition.check(entry.getKey(), entry.getValue()))
                return true
        }
        false
    }
}