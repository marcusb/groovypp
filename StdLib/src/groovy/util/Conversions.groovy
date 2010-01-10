package groovy.util

/**
 * Various methods to convert between standard data types.
 */
@Typed
abstract class Conversions {
  /**
   * Constructs the iterator view of the input array.
   */
  static <T> Iterator<T> iterator(T[] self) {
    return Arrays.asList(self).iterator();
  }

/**
   * Constructs the list out of the input iterator.
   */
  static <T> List<T> asList(Iterator<T> self) {
    def result = []
    while (self) {
      result << self.next()
    }
    return result;
  }
    /**
     * Constructs the list out of the input iterator.
     */
    static <T> List<T> asList(Iterable<T> self) {
        if (self instanceof List)
            self
        else {
            def result = []
            for (el in self)
                result << el
            result
        }
    }
}