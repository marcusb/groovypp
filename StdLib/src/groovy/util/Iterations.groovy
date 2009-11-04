package groovy.util

import org.codehaus.groovy.runtime.DefaultGroovyMethods

/**
 * Closure for iterations
 *
 * @param <T> type of iterated elements
 */
@Typed
abstract class Iterations {
    static <T,R> R each(Iterator<T> self, Function1<T,R> op) {
        R last = null
        while (self.hasNext()) {
            last = op.apply(self.next())
        }
        last
    }

    static <T,R> R each(Iterable<T> self, Function1<T,R> op) {
        each(self.iterator(), op)
    }

    static <T,R> R each(T[] self, Function1<T,R> op) {
        each(self.iterator(), op)
    }

    static <T,R> R each(Enumeration<T> self, Function1<T,R> op) {
        each(self.iterator(), op)
    }

    static <K,V,R> R each(Map<K,V> self, Function2<K,V,R> op) {
        R res = null
        def it = self.entrySet().iterator()
        while (it.hasNext()) {
            def el = it.next()
            res = op.apply (el.key, el.value)
        }
        res
    }

    static <T> T each(T self, Closure closure) {
        return DefaultGroovyMethods.each(self, closure)
    }

    static <T> Iterator<T> iterator(T[] self) {
        return Arrays.asList(self).iterator();
    }

    static <T> T[] toArray(Iterator<T> self) {
        ArrayList<T> list = new ArrayList<T>();
        while (self.hasNext())
            list.add(self.next());
        return (T[]) list.toArray();
    }

    static <T> T[] toArray(Iterable<T> self) {
        if (self instanceof Collection) {
            return (T[]) ((Collection<T>) self).toArray();
        } else {
            return toArray(self.iterator());
        }
    }
}
