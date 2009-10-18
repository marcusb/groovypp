package groovy.util;

/**
 * Closure for filtering
 *
 * @param <T> type of iterated elements
 */
public abstract class FilterClosure<T> {
    public abstract boolean check(T element);
}