package groovy.util;

public abstract class MapIterationClosureWithIndex<K, V> {
    public abstract void call(K key, V value, int index);
}