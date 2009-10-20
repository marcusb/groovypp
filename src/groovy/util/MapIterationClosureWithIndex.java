package groovy.util;

import java.util.Map;

public abstract class MapIterationClosureWithIndex<K, V> {
    public abstract void call(K key, V value, int index);

    public static <K, V> void each(Map<K, V> self, MapIterationClosureWithIndex<K, V> op) {
        int index = 0;
        for (Map.Entry<K, V> entry : self.entrySet()) {
            op.call(entry.getKey(), entry.getValue(), index++);
        }
    }
}