package groovy.util

/**
 * Closure for iterations
 *
 * @param <T> type of iterated elements
 */
abstract class ClosureWithState<T,S> {
    abstract S call(T element, S state)

    static <T,S> S each(Iterator<T> self, S state, ClosureWithState<T,S> op) {
        while (self.hasNext()) {
            T el = self.next()
            state = op.call(el, state)
        }
        state
    }

    static <T,S> S each(Iterable<T> self, S state, ClosureWithState<T,S> op) {
        each(self.iterator(), state, op)
    }

    static <K, V, S> S each(Map<K,V> self, S state, ClosureWithState<Map.Entry<K,V>,S> op) {
        each(self.entrySet().iterator(), state, op)
    }

    static <T,S> S each(T[] self, S state, ClosureWithState<T,S> op) {
        each(Arrays.asList(self), state, op)
    }

    static <K,S> S eachKey(Map<K,?> self, S state, ClosureWithState<K,S> op) {
        each(self.keySet().iterator(), state, op)
    }

    static <V,S> S eachValue(Map<?,V> self, S state, ClosureWithState<V,S> op) {
        each(self.values().iterator(), state, op)
    }
}
