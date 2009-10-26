package groovy.util

import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport

import java.util.Arrays
import java.util.Collection
import java.util.Iterator
import java.util.Map

/**
 * Closure for filtering
 *
 * @param <T> type of iterated elements
 */
@Typed
abstract class FilterClosure<T> extends DefaultGroovyMethodsSupport {

    abstract boolean checkCondition(T element)

    static <T> Iterator<T> filter(final Iterator<T> self, final FilterClosure<T> condition) {
        return new MyIterator<T> (self, condition)
    }

     static <T> T find(Iterator<T> self, FilterClosure<T> condition) {
        while (self.hasNext()) {
            T el = self.next()
            if (condition.checkCondition(el))
                return el
        }
        return null
    }

     static <T> T find(Iterable<T> self, FilterClosure<T> condition) {
        return find(self.iterator(), condition)
    }

     static <T> T find(T[] self, FilterClosure<T> condition) {
        return find(Arrays.asList(self), condition)
    }

     static <K,V> Map.Entry<K,V> find(Map<K,V> self, FilterClosure<Map.Entry<K,V>> condition) {
        return find(self.entrySet(), condition)
    }

     static <T> Collection<T> findAll(Iterator<T> self, Collection<T> answer, FilterClosure<T> condition) {
        while (self.hasNext()) {
            T value = self.next()
            if (condition.checkCondition(value)) {
                answer.add(value)
            }
        }
        return answer
    }

     static <K,V> Map<K,V> findAll(Map<K,V> self, FilterClosure<Map.Entry<K,V>> condition) {
        Map<K, V> map = createSimilarMap(self)
        for (Map.Entry<K, V> entry : self.entrySet()) {
            if (condition.checkCondition(entry))
                map.put(entry.getKey(), entry.getValue())
        }
        return map
    }

     static <T> Collection<T> findAll(Collection<T> self, FilterClosure<T> condition) {
        return findAll(self.iterator(), createSimilarCollection(self), condition)
    }

     static <T> Collection<T> findAll(T[] self, FilterClosure<T> condition) {
        return findAll(Arrays.asList(self), condition)
    }

     static <T> boolean any(Iterator<T> self, FilterClosure<T> condition) {
        while (self.hasNext()) {
            T value = self.next()
            if (condition.checkCondition(value)) {
                return true
            }
        }
        return false
    }

     static <K,V> boolean any(Map<K,V> self, FilterClosure<Map.Entry<K,V>> condition) {
        return any(self.entrySet(), condition)
    }

     static <T> boolean any(Collection<T> self, FilterClosure<T> condition) {
        return any(self.iterator(), condition)
    }

     static <T> boolean any(T [] self, FilterClosure<T> condition) {
        return any(Arrays.asList(self), condition)
    }

     static <T> boolean any(Iterable<T> self, FilterClosure<T> condition) {
        return any(self.iterator(), condition)
    }

    @Typed
    private static class MyIterator<T> implements Iterator<T> {

        private T next
            private boolean nextChecked
            private boolean hasNext

            private Iterator<T> self
            private FilterClosure<T> condition

            MyIterator (Iterator<T> self, FilterClosure<T> filter) {
                this.self = self;
                this.condition = filter;
            }

            boolean hasNext() {
                checkNext ()
                return hasNext
            }

            private void checkNext() {
                if (nextChecked)
                    return

                nextChecked = true

                hasNext = self.hasNext()
                if (hasNext) {
                    next = self.next()
                }
                while (hasNext && !condition.checkCondition(next)) {
                    hasNext = self.hasNext()
                    if (hasNext) {
                        next = self.next()
                    }
                }
            }

            T next() {
                checkNext()
                if (!hasNext)
                    throw new IllegalStateException("Iterator does not contain more elements")

                T res = next
                nextChecked = false
                next = null
                return res
            }

            void remove() {
                throw new UnsupportedOperationException("Iterator.remove() does not supported")
            }
        }
}