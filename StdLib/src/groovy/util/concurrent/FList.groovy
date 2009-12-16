package groovy.util.concurrent

/**
 * Simple implementation of one-directional immutable functional list
 */
@Typed
abstract static class FList<T> implements Iterable<T> {
    /**
     * Singleton for empty list
     */
    static final FList emptyList = new EmptyList ()

    /**
     * Number of elements in the list
     */
    final int size

    FList (int size) { this.@size = size }

    /**
     * Element last added to the list
     */
    abstract T getHead ()

    /**
     * Tail of the list
     */
    abstract FList<T> getTail ()

    /**
     * Check is this list empty
     */
    boolean isEmpty () { size == 0 }

    /**
     * Creates new list containing given element and then all element of this list
     */
    FList<T> add (T element) {
        new Node<T>(element, this)
    }

    /**
     * Utility method allowing convinient syntax <code>flist ()</code> for accessing head of the list
     */
    T call () { getHead() }

    /**
     * Create reversed copy of the list
     */
    final FList<T> reverse (FList<T> accumulated = FList.emptyList) {
        !size ? accumulated : tail.reverse(accumulated.add(head))
    }

    /**
     * Checks is this list contains given element
     */
    final boolean contains (T element) {
        size && (head == element || tail.contains(element))
    }

    private static class EmptyList<T> extends FList<T> {
        EmptyList () { super(0) }

        T        getHead () { throw new UnsupportedOperationException() }
        FList<T> getTail () { throw new UnsupportedOperationException() }

        Iterator iterator () { [hasNext:{false}, next:{throw new UnsupportedOperationException()}, remove:{throw new UnsupportedOperationException()}] }
    }

    private static class Node<T> extends FList<T> {
        final T        head
        final FList<T> tail

        Node (T head, FList<T> tail) {
            super (tail.size+1)
            this.@head = head
            this.@tail = tail
        }

        Iterator<T> iterator () {
            [
                cur:     (FList<T>)this,
                hasNext: { cur != FList.emptyList },
                next:    { def that = cur; cur = cur.tail; that.head },
                remove:  { throw new UnsupportedOperationException() }
            ]
        }
    }
}
