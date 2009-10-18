package groovy.util;

/**
 * Closure for iterations
 *
 * @param <T> type of iterated elements
 */
public abstract class IterationClosure<T> {
    public abstract void call(T element);
}
