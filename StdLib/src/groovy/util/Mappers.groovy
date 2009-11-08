package groovy.util

import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport

@Typed
public class Mappers extends DefaultGroovyMethodsSupport {

    private def Mappers() {}

    static <T,R> Collection<R> map(Collection<T> self, Function1<T,R> op) {
        def res = (Collection<R>) createSimilarCollection(self)
        for (T t : self) {
            res << op[t]
        }
        res
    }

    static <T,R> R[] map(T[] self, Function1<T,R> op) {
        def res = (R[]) new Object[self.length]
        for (int i = 0; i < res.length; i++) {
            res [i] = op[self[i]]
        }
        res
    }

    static <T,R> Iterator<R> map (Iterator<T> self, Function1<T,R> op ) {
        [ next: { op[self.next()] }, hasNext: { self.hasNext() }, remove: { self.remove() } ]
    }

    static <T,K> Map<K, List<T>> groupBy(Collection<T> self, Function1<T,K> op) {
        def answer = (Map<K, List<T>>)[:]
        for (T element : self) {
            def value = op.apply(element)
            def list = answer.get(value)
            if (list == null) {
                list = new LinkedList<T> ()
                answer[value] = list
            }
            list << element
        }
        answer
    }
}
