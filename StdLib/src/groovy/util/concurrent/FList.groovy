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

    FList (int size) { this.size = size }

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
    final boolean isEmpty () { size == 0 }

    final int size () { size }

    /**
     * Creates new list containing given element and then all element of this list
     */
    abstract FList<T> plus (T element)

    /**
     * Creates new list containing given element and then all element of this list
     */
    abstract FList<T> minus (T element)

    /**
     * Creates new list containing given element and then all element of this list
     */
    final FList<T> addAll (Iterable<T> elements) {
        def res = this
        for (el in elements)
            res += el
    }

    /**
     * Utility method allowing convinient syntax <code>flist ()</code> for accessing head of the list
     */
    final T call () { getHead() }

    /**
     * Create reversed copy of the list
     */
    final FList<T> reverse (FList<T> accumulated = FList.emptyList) {
        if(!size) { accumulated } else { tail.reverse(accumulated + head) }
    }

    /**
     * Checks is this list contains given element
     */
    final boolean contains (T element) {
        size && (head == element || tail.contains(element))
    }

    private static class EmptyList<T> extends FList<T> {
        EmptyList () { super(0) }

        Iterator iterator () {
            [
                hasNext:{false},
                next:{throw new NoSuchElementException()},
                remove:{throw new UnsupportedOperationException()}
            ]
        }

        final OneElementList<T> plus (T element) {
            [element]
        }

        FList<T> minus (T element) {
            throw new NoSuchElementException()
        }

        String toString () { "[]" }

        public T getHead() {
            throw new NoSuchElementException()
        }

        public FList<T> getTail() {
            throw new NoSuchElementException()
        }
    }

    private static class OneElementList<T> extends FList<T> {
        T head

        OneElementList (T head) {
            super(1)
            this.head = head
        }

        OneElementList (T head, int addSize) {
            super(addSize+1)
            this.head = head
        }

        final MoreThanOneElementList<T> plus(T element) {
            [element, this]
        }

        FList<T> minus (T element) {
            head == element ? tail : (tail-element) + head
        }

        public Iterator<T> iterator() {
            head.singleton().iterator()
        }

        public FList<T> getTail() {
            emptyList
        }
    }

    private static class MoreThanOneElementList<T> extends OneElementList<T> {
        final FList<T> tail

        MoreThanOneElementList (T head, FList<T> tail) {
            super (head, tail.size)
            this.tail = tail
        }

        Iterator<T> iterator () {
            [
                cur:     (FList<T>)this,
                hasNext: { cur.size },
                next:    { def that = cur; cur = cur.tail; that.head },
                remove:  { throw new UnsupportedOperationException() }
            ]
        }

        String toString () {
            def sb = new StringBuilder ()
            sb << "["
            toString(sb)
            sb << "]"
            sb.toString()
        }

        private void toString (StringBuilder sb) {
            sb << head
            if (!tail.empty) {
                sb << ", "
                ((MoreThanOneElementList)tail).toString(sb)
            }
        }
    }
}
