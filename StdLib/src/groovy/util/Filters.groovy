package groovy.util

import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport

/**
 * Utility methods for filtering.
 */
@Typed
class Filters extends DefaultGroovyMethodsSupport {

  private def Filters() {}

  static <T> Iterator<T> filter(final Iterator<T> self, final Function1<T, Boolean> condition) {
    return new MyIterator<T>(self, condition)
  }

  static <T> T find(Iterator<T> self, Function1<T, Boolean> condition) {
    while (self.hasNext()) {
      T el = self.next()
      if (condition.apply(el))
        return el
    }
    null
  }

  static <T> T find(Iterable<T> self, Function1<T, Boolean> condition) {
    find(self.iterator(), condition)
  }

  static <T> T find(T[] self, Function1<T, Boolean> condition) {
    find(Arrays.asList(self), condition)
  }

  static <K, V> Map.Entry<K, V> find(Map<K, V> self, Function2<K, V, Boolean> condition) {
    for (Map.Entry<K, V> entry: self.entrySet()) {
      if (condition.apply(entry.getKey(), entry.getValue()))
        return entry
    }
    null
  }

  static <T> Collection<T> findAll(Iterator<T> self, Collection<T> answer, Function1<T, Boolean> condition) {
    while (self.hasNext()) {
      T value = self.next()
      if (condition.apply(value)) {
        answer.add(value)
      }
    }
    return answer
  }

  static <K, V> Map<K, V> findAll(Map<K, V> self, Function2<K, V, Boolean> condition) {
    Map<K, V> map = createSimilarMap(self)
    for (Map.Entry<K, V> entry: self.entrySet()) {
      if (condition.apply(entry.getKey(), entry.getValue()))
        map.put(entry.getKey(), entry.getValue())
    }
    return map
  }

  static <T> Collection<T> findAll(Collection<T> self, Function1<T, Boolean> condition) {
    findAll(self.iterator(), createSimilarCollection(self), condition)
  }

  static <T> Collection<T> findAll(T[] self, Function1<T, Boolean> condition) {
    findAll(Arrays.asList(self), condition)
  }

  static <T> boolean any(Iterator<T> self, Function1<T, Boolean> condition) {
    while (self.hasNext()) {
      T value = self.next()
      if (condition.apply(value)) {
        return true
      }
    }
    false
  }

  static <K, V> boolean any(Map<K, V> self, Function2<K, V, Boolean> condition) {
    for (Map.Entry<K, V> entry : self.entrySet()) {
      if (condition.apply(entry.getKey(), entry.getValue())) {
        return true
      }
    }
    false
  }

  static <T> boolean any(Collection<T> self, Function1<T, Boolean> condition) {
    any(self.iterator(), condition)
  }

  static <T> boolean any(T[] self, Function1<T, Boolean> condition) {
    any(Arrays.asList(self), condition)
  }

  static <T> boolean any(Iterable<T> self, Function1<T, Boolean> condition) {
    any(self.iterator(), condition)
  }

  @Typed
  private static class MyIterator<T> implements Iterator<T> {
    private T next
    private boolean nextChecked
    private boolean hasNext

    private Iterator<T> self
    private Function1<T, Boolean> condition

    MyIterator(Iterator<T> self, Function1<T, Boolean> filter) {
      this.self = self;
      this.condition = filter;
    }

    boolean hasNext() {
      checkNext()
      hasNext
    }

    private void checkNext() {
      if (nextChecked)
        return

      nextChecked = true

      hasNext = self.hasNext()
      if (hasNext) {
        next = self.next()
      }
      while (hasNext && !condition.apply(next)) {
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
      res
    }

    void remove() {
      throw new UnsupportedOperationException("Iterator.remove() does not supported")
    }
  }
}