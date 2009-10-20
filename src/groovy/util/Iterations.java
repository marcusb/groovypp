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
}
