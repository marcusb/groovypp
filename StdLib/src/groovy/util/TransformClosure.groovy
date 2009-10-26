package groovy.util

import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport

import java.util.*

@Typed
public abstract class TransformClosure<T,R> extends DefaultGroovyMethodsSupport {
    abstract R transform(T element)

    static <T,R> Collection<R> transform(Collection<T> self, TransformClosure<T,R> transform) {
        def res = (Collection<R>) createSimilarCollection(self)
        for (T t : self) {
            res << (transform.transform(t))
        }
        res
    }

    static <T,R> R[] transform(T[] self, TransformClosure<T,R> transform) {
        def res = (R[]) new Object[self.length]
        for (int i = 0; i < res.length; i++) {
            res [i] = transform.transform(self[i])
        }
        res
    }

    static <T,R> R transform(T self, TransformClosure<T,R> transform) {
        transform.transform(self)
    }

    static <T,R> Iterator<R> transform(final Iterator<T> self, final TransformClosure<T,R> transform) {
        new MyIterator<T,R> (self, transform)
    }

    static <T,K> Map<K, List<T>> groupBy(Collection<T> self, TransformClosure<T,K> transform) {
        def answer = (Map<K, List<T>>)[:]
        for (T element : self) {
            def value = transform.transform(element)
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
        private final TransformClosure<T,R> transform

        MyIterator (Iterator<T> self, TransformClosure<T,R> transform) {
            this.self = self
            this.transform = transform
        }

        boolean hasNext() {
            self.hasNext()
        }

        R next() {
            transform.transform(self.next())
        }

        void remove() {
            self.remove()
        }
    }
}
