package groovy.util;

import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport;

import java.util.*;

/**
 * Utility methods for iterations of different collection-like objects
 */
public class Iterations extends DefaultGroovyMethodsSupport {

    private Iterations() {
        // no instantiation
    }

    public static <T> Iterator<T> iterator(T[] self) {
        return Arrays.asList(self).iterator();
    }

    public static <T> T[] toArray(Iterator<T> self) {
        ArrayList<T> list = new ArrayList<T>();
        while (self.hasNext())
            list.add(self.next());
        return (T[]) list.toArray();
    }

    public static <T> T[] toArray(Iterable<T> self) {
        if (self instanceof Collection) {
            return (T[]) ((Collection<T>) self).toArray();
        } else {
            return toArray(self.iterator());
        }
    }

    public static <T> void each(Iterator<T> self, IterationClosure<T> op) {
        while (self.hasNext()) {
            T el = self.next();
            op.call(el);
        }
    }

    public static <T> void each(Iterable<T> self, IterationClosure<T> op) {
        each(self.iterator(), op);
    }

    public static <K, V> void each(Map<K, V> self, IterationClosure<Map.Entry<K, V>> op) {
        each(self.entrySet().iterator(), op);
    }

    public static <T> void each(T[] self, IterationClosure<T> op) {
        each(Arrays.asList(self), op);
    }

    public static <K> void eachKey(Map<K, ?> self, IterationClosure<K> op) {
        each(self.keySet().iterator(), op);
    }

    public static <V> void eachValue(Map<?, V> self, IterationClosure<V> op) {
        each(self.values().iterator(), op);
    }

    public static <T> void each(Iterator<T> self, IterationClosureWithIndex<T> op) {
        for (int index = 0; self.hasNext(); index++) {
            T el = self.next();
            op.call(el, index);
        }
    }

    public static <T> void each(Iterable<T> self, IterationClosureWithIndex<T> op) {
        each(self.iterator(), op);
    }

    public static <K, V> void each(Map<K, V> self, IterationClosureWithIndex<Map.Entry<K, V>> op) {
        each(self.entrySet().iterator(), op);
    }

    public static <T> void each(T[] self, IterationClosureWithIndex<T> op) {
        each(Arrays.asList(self), op);
    }

    public static <K> void eachKey(Map<K, ?> self, IterationClosureWithIndex<K> op) {
        each(self.keySet().iterator(), op);
    }

    public static <V> void eachValue(Map<?, V> self, IterationClosureWithIndex<V> op) {
        each(self.values().iterator(), op);
    }

    public static <K, V> void each(Map<K, V> self, MapIterationClosureWithIndex<K, V> op) {
        int index = 0;
        for (Map.Entry<K, V> entry : self.entrySet()) {
            op.call(entry.getKey(), entry.getValue(), index++);
        }
    }

    public static <K, V> void each(Map<K, V> self, MapIterationClosure<K, V> op) {
        for (Map.Entry<K, V> entry : self.entrySet()) {
            op.call(entry.getKey(), entry.getValue());
        }
    }

    public static <T> T find(Iterator<T> self, FilterClosure<T> condition) {
        while (self.hasNext()) {
            T el = self.next();
            if (condition.check(el))
                return el;
        }
        return null;
    }

    public static <T> T find(Iterable<T> self, FilterClosure<T> condition) {
        return find(self.iterator(), condition);
    }

    public static <T> T find(T[] self, FilterClosure<T> condition) {
        return find(Arrays.asList(self), condition);
    }

    public static <T> Collection<T> findAll(Iterator<T> self, Collection<T> answer, FilterClosure<T> condition) {
        while (self.hasNext()) {
            T value = self.next();
            if (condition.check(value)) {
                answer.add(value);
            }
        }
        return answer;
    }

    public static <T> Collection<T> findAll(Collection<T> self, FilterClosure<T> condition) {
        return findAll(self.iterator(), createSimilarCollection(self), condition);
    }

    public static <T> Collection<T> findAll(T[] self, FilterClosure<T> condition) {
        return findAll(Arrays.asList(self), condition);
    }
}
