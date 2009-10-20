package groovy.util;

import java.util.Map;

public abstract class MapIterationClosure<K, V> {
    public abstract void call(K key, V value);

    public static <K, V> void each(Map<K, V> self, MapIterationClosure<K, V> op) {
        for (Map.Entry<K, V> entry : self.entrySet()) {
            op.call(entry.getKey(), entry.getValue());
        }
    }
}