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
   * Obtains all the elements from the input iterator and returns them in the array.
   */
  static <T> T[] toArray(Iterator<T> self) {
    ArrayList<T> list = new ArrayList<T>();
    while (self.hasNext())
      list.add(self.next());
    return (T[]) list.toArray();
  }

  /**
   * Obtains all the elements from the input @link{Iterable} object and returns them in the array.
   */
  static <T> T[] toArray(Iterable<T> self) {
    if (self instanceof Collection) {
      return (T[]) ((Collection<T>) self).toArray();
    } else {
      return toArray(self.iterator());
    }
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
}