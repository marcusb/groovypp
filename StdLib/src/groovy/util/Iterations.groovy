package groovy.util

import org.codehaus.groovy.runtime.DefaultGroovyMethods

/**
 */
@Typed
abstract class Iterations {
    static <T,R> R foldLeft(Iterator<T> self, R init, Function2<T,R,R> op) {
        self.hasNext() ? foldLeft(self, op.apply(self.next(), init), op) : init
    }

    // Not tail-recursive!!!
    static <T,R> R foldRight(Iterator<T> self, R init, Function2<T,R,R> op) {
        self.hasNext() ? op.apply(self.next(), foldRight(self, init, op)) : init
    }

    static <T,R> R each(Iterator<T> self, Function1<T,R> op, R last = null) {
        self.hasNext() ? each(self, op, op.apply(self.next())) : last
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

    private static <K,V,R> R eachMap(Iterator<Map.Entry<K,V>> it, Function2<K,V,R> op, R last) {
        if (!it.hasNext())
           last
        else {
            def el = it.next()
            eachMap(it, op, op.apply (el.key, el.value))
        }
    }

    static <K,V,R> R each(Map<K,V> self, Function2<K,V,R> op) {
        eachMap(self.entrySet().iterator(), op, null)
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
