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