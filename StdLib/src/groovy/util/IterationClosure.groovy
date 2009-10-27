package groovy.util

/**
 * Closure for iterations
 *
 * @param <T> type of iterated elements
 */
abstract class IterationClosure<T> {
    abstract void call(T element)

    static <T> void each(Iterator<T> self, IterationClosure<T> op) {
        while (self.hasNext()) {
            T el = self.next()
            op.call(el)
        }
    }

    static <T> void each(Iterable<T> self, IterationClosure<T> op) {
        each(self.iterator(), op)
    }

    static <K, V> void each(Map<K, V> self, IterationClosure<Map.Entry<K, V>> op) {
        each(self.entrySet().iterator(), op)
    }

    static <T> void each(T[] self, IterationClosure<T> op) {
        each(Arrays.asList(self), op)
    }

    static <K> void eachKey(Map<K, ?> self, IterationClosure<K> op) {
        each(self.keySet().iterator(), op)
    }

    static <V> void eachValue(Map<?, V> self, IterationClosure<V> op) {
        each(self.values().iterator(), op)
    }
}
