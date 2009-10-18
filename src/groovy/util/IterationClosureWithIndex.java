package groovy.util;

public abstract class IterationClosureWithIndex<T> {
    public abstract void call(T element, int index);
}