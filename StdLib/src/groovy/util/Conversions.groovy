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