package groovy.util

/**
 * Closure for iterations
 *
 * @param <T> type of iterated elements
 */
@Typed
abstract class Iterations {
    static <T,R> R each(Iterator<T> self, Function1<T,R> op) {
        R res = null
        while (self.hasNext()) {
            res = op.apply(self.next())
        }
        res
    }

    static <T,R> void each(Iterable<T> self, Function1<T,R> op) {
        each(self.iterator(), op)
    }

    static <T,R> void each(T[] self, Function1<T,R> op) {
        each(self.iterator(), op)
    }

    static <T,R> void each(Enumeration<T> self, Function1<T,R> op) {
        each(self.iterator(), op)
    }

    static <K,V,R> void each(Map<K,V> self, Function2<K,V,R> op) {
        R res = null
        def it = self.entrySet().iterator()
        while (it.hasNext()) {
            def el = it.next()
            res = op.apply (el.key, el.value)
        }
        res
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
