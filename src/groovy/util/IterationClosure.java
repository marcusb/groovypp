package groovy.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Arrays;

/**
 * Closure for iterations
 *
 * @param <T> type of iterated elements
 */
public abstract class IterationClosure<T> {
    public abstract void call(T element);

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
}
