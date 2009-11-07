package groovy.util

import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport

@Typed
public class Mappers extends DefaultGroovyMethodsSupport {

    private def Mappers() {}

    static <T,R> Collection<R> map(Collection<T> self, Function1<T,R> op) {
        def res = (Collection<R>) createSimilarCollection(self)
        for (T t : self) {
            res << (op.apply(t))
        }
        res
    }

    static <T,R> R[] map(T[] self, Function1<T,R> op) {
        def res = (R[]) new Object[self.length]
        for (int i = 0; i < res.length; i++) {
            res [i] = op.apply(self[i])
        }
        res
    }

    static <T,R> Iterator<R> map(final Iterator<T> self, final Function1<T,R> op) {
        new MyIterator<T,R> (self, op)
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

    @Typed
    private static class MyIterator<T,R> implements Iterator<R> {
        private final Iterator<T> self
        private final Function1<T,R> op

        MyIterator (Iterator<T> self, Function1<T,R> op) {
            this.self = self
            this.op = op
        }

        boolean hasNext() {
            self.hasNext()
        }

        R next() {
            op.apply(self.next())
        }

        void remove() {
            self.remove()
        }
    }
}
