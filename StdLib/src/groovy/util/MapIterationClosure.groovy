package groovy.util;

abstract class MapIterationClosure<K, V> {
    abstract void call(K key, V value);

    static <K, V> void each(Map<K, V> self, MapIterationClosure<K, V> op) {
        for (Map.Entry<K, V> entry : self.entrySet()) {
            op.call(entry.getKey(), entry.getValue());
        }
    }
}