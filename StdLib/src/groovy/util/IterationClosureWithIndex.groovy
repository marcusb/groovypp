package groovy.util

abstract class IterationClosureWithIndex<T> {
    abstract void call(T element, int index)

    static <T> void each(Iterator<T> self, IterationClosureWithIndex<T> op) {
        for (int index = 0; self.hasNext(); index++) {
            T el = self.next()
            op.call(el, index)
        }
    }

    static <T> void each(Iterable<T> self, IterationClosureWithIndex<T> op) {
        each(self.iterator(), op)
    }

    static <K, V> void each(Map<K, V> self, IterationClosureWithIndex<Map.Entry<K, V>> op) {
        each(self.entrySet().iterator(), op)
    }

    static <T> void each(T[] self, IterationClosureWithIndex<T> op) {
        each(Arrays.asList(self), op)
    }

    static <K> void eachKey(Map<K, ?> self, IterationClosureWithIndex<K> op) {
        each(self.keySet().iterator(), op)
    }

    static <V> void eachValue(Map<?, V> self, IterationClosureWithIndex<V> op) {
        each(self.values().iterator(), op)
    }
}