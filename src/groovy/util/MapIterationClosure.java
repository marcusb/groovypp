package groovy.util;

public abstract class MapIterationClosure<K, V> {
    public abstract void call(K key, V value);
}