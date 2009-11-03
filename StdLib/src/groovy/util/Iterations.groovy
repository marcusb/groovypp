package groovy.util

/**
 * Closure for iterations
 *
 * @param <T> type of iterated elements
 */
abstract class Iterations {
    static <T> void each(Iterator<T> self, Function1<T,Void> op) {
        while (self.hasNext()) {
            T el = self.next()
            op.call(el)
        }
    }

    static <T,S> S each(Iterator<T> self, S initState, Function2<T,S,S> op) {
        def state = initState
        while (self.hasNext()) {
            T el = self.next()
            state = op.call(el, state)
        }
        state
    }

    static <K,V> void eachEntry(Map<K,V> self, Function2<K,V,Void> op) {
        while (self.entrySet().hasNext()) {
            Map.Entry<K,V> el = self.next()
            op.call el.key, el.value
        }
    }

    static <K,V,S> void eachEntry(Map<K,V> self, S initState, Function3<K,V,S,S> op) {
        def state = initState
        while (self.entrySet().hasNext()) {
            Map.Entry<K,V> el = self.next()
            state = op.call(el.key, el.value, state)
        }
        state
    }

    static <T> void each(Iterable<T> self, Function1<T,Void> op) {
        each(self.iterator(), op)
    }

    static <T,S> S each(Iterable<T> self, S initState, Function2<T,S,S> op) {
        each(self.iterator(), initState, op)
    }

    static <K, V> void eachEntry(Map<K, V> self, Function1<Map.Entry<K, V>,Void> op) {
        each(self.entrySet().iterator(), op)
    }

    static <K, V, S> S eachEntry(Map<K, V> self, S initState, Function2<Map.Entry<K, V>,S,S> op) {
        each(self.entrySet().iterator(), initState, op)
    }

    static <T> void each(T[] self, Function1<T,Void> op) {
        each(Arrays.asList(self), op)
    }

    static <T,S> S each(T[] self, S initState, Function2<T,S,S> op) {
        each(Arrays.asList(self), initState, op)
    }

    static <K> void eachKey(Map<K, ?> self, Function1<K,Void> op) {
        each(self.keySet().iterator(), op)
    }

    static <K,S> S eachKey(Map<K, ?> self, S initState, Function2<K,S,S> op) {
        each(self.keySet().iterator(), initState, op)
    }

    static <V> void eachValue(Map<?, V> self, Function1<V,Void> op) {
        each(self.values().iterator(), op)
    }

    static <V,S> S eachValue(Map<?, V> self, S initValue, Function2<V,S,S> op) {
        each(self.values().iterator(), initValue, op)
    }

    static <T> void each(Iterator<T> self, Function2<T,Integer,Void> op) {
        for (int index = 0; self.hasNext(); index++) {
            T el = self.next()
            op.call(el, index)
        }
    }

    static <T,S> S each(Iterator<T> self, S initState, Function3<T,S,Integer,S> op) {
        def state = initState
        for (int index = 0; self.hasNext(); index++) {
            T el = self.next()
            state = op.call(el, state, index)
        }
        state
    }

    static <T> void each(Iterable<T> self, Function2<T,Integer,Void> op) {
        each(self.iterator(), op)
    }

    static <T,S> S each(Iterable<T> self, S initState, Function3<T,S,Integer,S> op) {
        each(self.iterator(), initState, op)
    }

    static <K, V> void each(Map<K, V> self, Function2<Map.Entry<K, V>,Integer,Void> op) {
        each(self.entrySet().iterator(), op)
    }

    static <K, V, S> S each(Map<K, V> self, S initState, Function3<Map.Entry<K, V>,S,Integer,S> op) {
        each(self.entrySet().iterator(), initState, op)
    }

    static <T> void each(T[] self, Function2<T,Integer,Void> op) {
        each(Arrays.asList(self), op)
    }

    static <T,S> S each(T[] self, S initState, Function3<T,S, Integer,S> op) {
        each(Arrays.asList(self), initState, op)
    }

    static <K> void eachKey(Map<K, ?> self, Function2<K,Integer,Void> op) {
        each(self.keySet().iterator(), op)
    }

    static <K,S> S eachKey(Map<K, ?> self, S initState, Function3<K,S,Integer,S> op) {
        each(self.keySet().iterator(), initState, op)
    }

    static <V> void eachValue(Map<?, V> self, Function2<V,Integer,Void> op) {
        each(self.values().iterator(), op)
    }

    static <V,S> S eachValue(Map<?, V> self, S initState, Function3<V,S,Integer,S> op) {
        each(self.values().iterator(), initState, op)
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
}
