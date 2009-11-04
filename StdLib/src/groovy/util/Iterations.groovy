package groovy.util

/**
 * Closure for iterations
 *
 * @param <T> type of iterated elements
 */
@Typed
abstract class Iterations {
    static <T> void each(Iterator<T> self, Function1<T,Object> op) {
        while (self.hasNext()) {
            op.call(self.next())
        }
    }

    static <T,S> S each(Iterator<T> self, S initState, Function2<T,S,S> op) {
        def state = initState
        while (self.hasNext()) {
            state = op.call(self.next(), state)
        }
        state
    }

    static <T> void each(Iterable<T> self, Function1<T,Object> op) {
        each(self.iterator(), op)
    }

    static <T,S> S each(Iterable<T> self, S initState, Function2<T,S,S> op) {
        each(self.iterator(), initState, op)
    }

    static <T> void each(T[] self, Function1<T,Object> op) {
        each(Arrays.asList(self), op)
    }

    static <T,S> S each(T[] self, S initState, Function2<T,S,S> op) {
        each(Arrays.asList(self), initState, op)
    }

    static <T> void eachWithIndex(Iterator<T> self, Function2<T,Integer,Object> op) {
        for (int index = 0; self.hasNext(); index++) {
            op.call(self.next(), index)
        }
    }

    static <T,S> S eachWithIndex(Iterator<T> self, S initState, Function3<T,S,Integer,S> op) {
        def state = initState
        for (int index = 0; self.hasNext(); index++) {
            state = op.call(self.next(), state, index)
        }
        state
    }

    static <T> void eachWithIndex(Iterable<T> self, Function2<T,Integer,Object> op) {
        eachWithIndex(self.iterator(), op)
    }

    static <T,S> S eachWithIndex(Iterable<T> self, S initState, Function3<T,S,Integer,S> op) {
        eachWithIndex(self.iterator(), initState, op)
    }

    static <K, V> void eachWithIndex(Map<K, V> self, Function2<Map.Entry<K, V>,Integer,Object> op) {
        eachWithIndex(self.entrySet().iterator(), op)
    }

    static <K, V, S> S eachWithIndex(Map<K, V> self, S initState, Function3<Map.Entry<K, V>,S,Integer,S> op) {
        eachWithIndex(self.entrySet().iterator(), initState, op)
    }

    static <T> void eachWithIndex(T[] self, Function2<T,Integer,Object> op) {
        eachWithIndex(Arrays.asList(self), op)
    }

    static <T,S> S eachWithIndex(T[] self, S initState, Function3<T,S, Integer,S> op) {
        eachWithIndex(Arrays.asList(self), initState, op)
    }

    static <K,V> void eachKeyValue(Map<K,V> self, Function2<K,V,Object> op) {
        def it = self.entrySet().iterator()
        while (it.hasNext()) {
            def el = it.next()
            op.call el.key, el.value
        }
    }

    static <K,V,S> S eachKeyValue(Map<K,V> self, S initState, Function3<K,V,S,S> op) {
        def state = initState
        def it = self.entrySet().iterator()
        while (it.hasNext()) {
            def el = it.next()
            state = op.call(el.key, el.value, state)
        }
        state
    }

    static <K,V> void eachKeyValueWithIndex(Map<K,V> self, Function3<K,V,Integer,Object> op) {
        def index = 0
        def it = self.entrySet().iterator()
        while (it.hasNext()) {
            def el = it.next()
            op.call el.key, el.value, index++
        }
    }

    static <K,V,S> S eachKeyValueWithIndex(Map<K,V> self, S initState, Function4<K,V,S,Integer,S> op) {
        def index = 0
        def state = initState
        def it = self.entrySet().iterator()
        while (it.hasNext()) {
            def el = it.next()
            state = op.call(el.key, el.value, state, index++)
        }
        state
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
