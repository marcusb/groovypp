package groovy.util.concurrent

@Typed
abstract class FQueue<T> implements Iterable<T> {
    final boolean isEmpty () { size == 0 }

    T getFirst () { throw new NoSuchElementException() }

    Pair<T, FQueue<T>> removeFirst() { throw new NoSuchElementException() }

    abstract FQueue<T> addLast (T element)

    abstract FQueue<T> addFirst (T element)

    /**
     * Number of elements in the list
     */
    final int size

    static final EmptyQueue emptyQueue = []

    FQueue (int size) {
        this.size = size
    }

    private static class EmptyQueue<T> extends FQueue<T> {
        EmptyQueue(){
            super(0)
        }

        NonEmptyQueue<T> addLast (T element)  { [element, FList.emptyList, FList.emptyList] }

        NonEmptyQueue<T> addFirst (T element) { [element, FList.emptyList, FList.emptyList] }

        Iterator<T> iterator () {
            [
                    hasNext: { false },
                    next:    { throw new UnsupportedOperationException() },
                    remove:  { throw new UnsupportedOperationException() }
            ]
        }

        String toString () {
            "[,[],[]]"
        }
    }

    private static class NonEmptyQueue<T> extends FQueue<T> {
        private final FList<T> input, output
        private final T head

        NonEmptyQueue (T head, FList<T> output, FList<T> input) {
            super(input.size + output.size + 1)
            this.input  = input
            this.output = output
            this.head   = head
        }

        NonEmptyQueue<T> addLast (T element) {
            [head, output, input + element]
        }

        NonEmptyQueue<T> addFirst (T element) {
            [element, output + head, input]
        }

        T getFirst () { head }

        Pair<T, FQueue<T>> removeFirst() {
            if (size == 1)
                [head, FQueue.emptyQueue]
            else {
                if(!output.empty)
                    [head, (NonEmptyQueue<T>)[output.head, output.tail, input]]
                else {
                    FList<T> newOut = input.reverse(FList.emptyList)
                    [head, (NonEmptyQueue<T>)[newOut.head, newOut.tail, FList.emptyList]]
                }
            }
        }

        Iterator<T> iterator () {
            head.singleton().iterator() | output.iterator() | input.reverse(FList.emptyList).iterator()
        }

        String toString () {
            "[$head,$output,$input]"
        }

    }
}