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

@Typed class Sort {

    /**
     * Sorts the given map into a sorted map using
     * given comparator.
     * @param self the map to be sorted
     * @param @link{java.util.Comparator} comparator
     * @return @link{java.util.LinkedHashMap} where entres are added in the sort order.
     */
    static <K, V> LinkedHashMap<K, V> sort(Map<K, V> self, Comparator<Map.Entry<K,V>> comparator) {
        def result = new LinkedHashMap<K, V>()
        def entries = self.entrySet().asList()
        entries.sort(comparator)
        for (entry in entries) {
            result[entry.key] = entry.value
        }
        result
    }

    /**
     * Sorts the given map into a sorted map using
     * given projection.
     * @param self the map to be sorted
     * @param @link{java.util.Comparator} comparator
     * @return @link{java.util.LinkedHashMap} where entres are added in the sort order.
     */
    static <K, V> LinkedHashMap<K, V> sort(Map<K, V> self, Projection<Map.Entry<K,V>> projection) {
        self.sort(projection.comparator())
    }

    static abstract class Projection<T> {
        Comparator<T> comparator() {
            { o1, o2 -> project(o1).compareTo(project(o2)) }
        }

        abstract Comparable project(T obj)
    }
}