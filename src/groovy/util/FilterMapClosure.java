package groovy.util;

import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport;

import java.util.Map;

/**
 * Closure for filtering
 *
 */
public abstract class FilterMapClosure<K,V> extends DefaultGroovyMethodsSupport {
    public abstract boolean check(K key, V value);

    public static <K,V> Map.Entry<K,V> find(Map<K,V> self, FilterMapClosure<K,V> condition) {
        for (Map.Entry<K, V> entry : self.entrySet()) {
            if (condition.check(entry.getKey(), entry.getValue()))
                return entry;
        }
        return null;
    }

    public static <K,V> Map<K,V> findAll(Map<K,V> self, FilterMapClosure<K,V> condition) {
        Map<K, V> map = createSimilarMap(self);
        for (Map.Entry<K, V> entry : self.entrySet()) {
            if (condition.check(entry.getKey(), entry.getValue()))
                map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    public static <K,V> boolean any(Map<K,V> self, FilterMapClosure<K,V> condition) {
        for (Map.Entry<K, V> entry : self.entrySet()) {
            if (condition.check(entry.getKey(), entry.getValue()))
                return true;
        }
        return false;
    }
}