package groovy.util

import java.util.regex.Matcher
import java.util.regex.Pattern
import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport
import org.codehaus.groovy.runtime.InvokerHelper

/**
 * Utility methods for filtering.
 */
@Typed
class Filters extends DefaultGroovyMethodsSupport {

  private def Filters() {}

  /**
   * Filters all elements that satisfy the input condition.
   * @param self the iterator to take elements from.
   * @param condition filter predicate.
   * @returns iterator containing elements that satisfy the condition.
   */
  static <T> Iterator<T> filter(final Iterator<T> self, final Function1<T, Boolean> condition) {
      [
              nextElem :   (T)null,
              nextChecked: false,
              nextFound:   false,

              checkNext: { ->
                  if (!nextChecked) {
                      nextChecked = true

                      nextFound = self.hasNext()
                      if (nextFound) {
                        nextElem = self.next()
                      }
                      while (nextFound && !condition.call(nextElem)) {
                        nextFound = self.hasNext()
                        if (nextFound) {
                          nextElem = self.next()
                        }
                      }
                  }
              },

              hasNext : { -> checkNext(); nextFound },

              next : { ->
                  checkNext()
                  if (!nextFound)
                    throw new IllegalStateException("Iterator does not contain more elements")

                  T res = nextElem
                  nextChecked = false
                  nextElem = null
                  res
              },

              remove : { -> throw new UnsupportedOperationException("remove () is unsupported by the iterator") }
      ]
  }

  /**
   * Filters all elements that satisfy the input condition.
   * @param self the list to take elements from.
   * @param condition filter predicate.
   * @returns list containing elements that satisfy the condition.
   */
  static <T> List<T> filter(List<T> self, final Function1<T, Boolean> condition) {
    self.iterator().filter(condition).asList()
  }

  /**
   * Finds first element that satisfies the input condition.
   * @param self the iterator to take elements from.
   * @param condition filter predicate.
   * @returns first element that satisfies the condition.
   */
  static <T> T find(Iterator<T> self, Function1<T, Boolean> condition) {
    while (self.hasNext()) {
      T el = self.next()
      if (condition.call(el))
        return el
    }
    null
  }

  /**
   * Finds first element that satisfies the input condition.
   * @param self the @link{Iterable} object to take elements from.
   * @param condition filter predicate.
   * @returns first element that satisfies the condition.
   */
  static <T> T find(Iterable<T> self, Function1<T, Boolean> condition) {
    find(self.iterator(), condition)
  }

  /**
   * Finds first element that satisfies the input condition.
   * @param self the array to take elements from.
   * @param condition filter predicate.
   * @returns first element that satisfies the condition.
   */
  static <T> T find(T[] self, Function1<T, Boolean> condition) {
    find(Arrays.asList(self), condition)
  }

  // String-related stuff copied from DGM.
  public static String find(String self, String regex) {
      return find(self, Pattern.compile(regex));
  }

  public static String find(String self, Pattern pattern) {
    Matcher matcher = pattern.matcher(self);
    if (matcher.find()) {
      return matcher.group(0);
    }
    return null;
  }

  public static String find(String self, String regex, Closure closure) {
      return find(self, Pattern.compile(regex), closure);
  }

  public static String find(String self, Pattern pattern, Closure closure) {
      Matcher matcher = pattern.matcher(self);
      if (matcher.find()) {
          if (matcher.hasGroup()) {
              int count = matcher.groupCount();
              List groups = new ArrayList(count);
              for (int i = 0; i <= count; i++) {
                  groups.add(matcher.group(i));
              }
              return InvokerHelper.toString(closure.call(groups));
          } else {
              return InvokerHelper.toString(closure.call(matcher.group(0)));
          }
      }
      return null;
  }

  /**
   * Finds first element that satisfies the input condition.
   * @param self the map to take elements from.
   * @param condition filter predicate.
   * @returns first element that satisfies the condition.
   */
  static <K, V> Map.Entry<K, V> find(Map<K, V> self, Function2<K, V, Boolean> condition) {
    for (Map.Entry<K, V> entry: self.entrySet()) {
      if (condition.call(entry.getKey(), entry.getValue()))
        return entry
    }
    null
  }

  /**
   * Finds all elements that satisfy the input condition.
   * @param self the iterator to take elements from.
   * @param condition filter predicate.
   * @returns collection of elements that satisfy the condition.
   */
  static <T> Collection<T> findAll(Iterator<T> self, Collection<T> answer, Function1<T, Boolean> condition) {
    while (self.hasNext()) {
      T value = self.next()
      if (condition.call(value)) {
        answer.add(value)
      }
    }
    return answer
  }

  /**
   * Finds all elements that satisfy the input condition.
   * @param self the map to take elements from.
   * @param condition filter predicate.
   * @returns map of elements that satisfy the condition.
   */
  static <K, V> Map<K, V> findAll(Map<K, V> self, Function2<K, V, Boolean> condition) {
    Map<K, V> map = createSimilarMap(self)
    for (Map.Entry<K, V> entry: self.entrySet()) {
      if (condition.call(entry.getKey(), entry.getValue()))
        map.put(entry.getKey(), entry.getValue())
    }
    return map
  }

  /**
   * Finds all elements that satisfy the input condition.
   * @param self @link{java.util.Collection} to take elements from.
   * @param condition filter predicate.
   * @returns collection of elements that satisfy the condition.
   */
  static <T> Collection<T> findAll(Collection<T> self, Function1<T, Boolean> condition) {
    findAll(self.iterator(), createSimilarCollection(self), condition)
  }

  /**
   * Finds all elements that satisfy the input condition.
   * @param self the array to take elements from.
   * @param condition filter predicate.
   * @returns collection of elements that satisfy the condition.
   */
  static <T> Collection<T> findAll(T[] self, Function1<T, Boolean> condition) {
    findAll(Arrays.asList(self), condition)
  }

  /**
   * Checks if there are elements that satisfy the input condition.
   * @param self the iterator to take elements from.
   * @param condition filter predicate.
   * @returns true if there is at least one element that satisfies the input condition.
   */
  static <T> boolean any(Iterator<T> self, Function1<T, Boolean> condition) {
    while (self.hasNext()) {
      T value = self.next()
      if (condition.call(value)) {
        return true
      }
    }
    false
  }

  /**
   * Checks if there are elements that satisfy the input condition.
   * @param self the map to take elements from.
   * @param condition filter predicate.
   * @returns true if there is at least one element that satisfies the input condition.
   */
  static <K, V> boolean any(Map<K, V> self, Function2<K, V, Boolean> condition) {
    for (Map.Entry<K, V> entry : self.entrySet()) {
      if (condition.call(entry.getKey(), entry.getValue())) {
        return true
      }
    }
    false
  }

  /**
   * Checks if there are elements that satisfy the input condition.
   * @param self the array to take elements from.
   * @param condition filter predicate.
   * @returns true if there is at least one element that satisfies the input condition.
   */
  static <T> boolean any(T[] self, Function1<T, Boolean> condition) {
    any(Arrays.asList(self), condition)
  }

  /**
   * Checks if there are elements that satisfy the input condition.
   * @param self @link{Iterable} object to take elements from.
   * @param condition filter predicate.
   * @returns true if there is at least one element that satisfies the input condition.
   */
  static <T> boolean any(Iterable<T> self, Function1<T, Boolean> condition) {
    any(self.iterator(), condition)
  }
}