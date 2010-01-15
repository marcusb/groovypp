package groovy.util

@Typed public class Sort {

    /**
     * Sorts the given map into a sorted map using
     * given comparator.
     * @param self the map to be sorted
     * @param @link{java.util.Comparator} comparator
     * @return @link{java.util.LinkedHashMap} where entres are added in the sort order.
     */
    public static <K, V> LinkedHashMap<K, V> sort(Map<K, V> self, Comparator<Map.Entry<K,V>> comparator) {
        def result = new LinkedHashMap<K, V>()
        def entries = self.entrySet().asList()
        entries.sort(comparator)
        for (entry in entries) {
            result[entry.key] = entry.value
        }
        result
    }

    /**
     * Sorts the given map into a sorted map using a function mapping entries to comparable objects.
     * @param self the map to be sorted
     * @param projection a function mapping entries to comparable objects.
     * @return @link{java.util.LinkedHashMap} where entres are added in the sort order.
     */
    static <K, V, R extends Comparable> LinkedHashMap<K, V> sort(Map<K, V> self, Function1<Map.Entry<K,V>,R> projection) {
        def result = new LinkedHashMap<K, V>()
        def entries = self.entrySet().asList()
        entries.sort(new ProjectionComparator<Map.Entry<K,V>,R> (projection));
        for (entry in entries) {
            result[entry.key] = entry.value
        }
        result
    }

    private static class ProjectionComparator<T,R extends Comparable> implements Comparator<T> {
        Function1<T,R> projection

        ProjectionComparator (Function1<T,R> projection) {
            this.@projection = projection
        }

        public int compare(T o1, T o2) {
            projection.call(o1) <=> projection.call(o2)
        }
    }
}