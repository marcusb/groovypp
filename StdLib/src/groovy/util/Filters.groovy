/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package groovy.util

import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport

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
  static <T> Iterator<T> filter(List<T> self, final Function1<T, Boolean> condition) {
    self.iterator().filter(condition)
  }

  static <T> Iterator<T> filter(T[] self, final Function1<T, Boolean> condition) {
    filter(self.asList(), condition)
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

  static boolean isCase(Iterator self, Object obj) {
    self.any { obj == it }
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