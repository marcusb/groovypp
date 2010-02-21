package groovy.util.concurrent

@Typed
abstract class FQueue<T> implements Iterable<T> {
    final boolean isEmpty () { !size() }

    T getFirst () { throw new NoSuchElementException() }

    Pair<T, FQueue<T>> removeFirst() { throw new NoSuchElementException() }

    abstract FQueue<T> addLast (T element)

    abstract FQueue<T> addFirst (T element)

    /**
     * Number of elements in the list
     */

    static final EmptyQueue emptyQueue = []

    FQueue () {
    }

    abstract int size ()

    private static final class EmptyQueue<T> extends FQueue<T> {
        EmptyQueue(){
        }

        NonEmptyQueue<T> addLast (T element)  { [FList.emptyList + element, FList.emptyList] }

        NonEmptyQueue<T> addFirst (T element) { [FList.emptyList + element, FList.emptyList] }

        Iterator<T> iterator () {
            [
                    hasNext: { false },
                    next:    { throw new UnsupportedOperationException() },
                    remove:  { throw new UnsupportedOperationException() }
            ]
        }

        String toString () {
            "[[],[]]"
        }

        final int size () { 0 }
    }

    private static final class NonEmptyQueue<T> extends FQueue<T> {
        private final FList<T> input, output

        NonEmptyQueue (FList<T> output, FList<T> input) {
            this.input  = input
            this.output = output
        }

        NonEmptyQueue<T> addLast (T element) {
            [output, input + element]
        }

        NonEmptyQueue<T> addFirst (T element) {
            [output + element, input]
        }

        T getFirst () { output.head }

        Pair<T, FQueue<T>> removeFirst() {
            if (size () == 1)
                [output.head, FQueue.emptyQueue]
            else {
                if(output.size > 1)
                    [output.head, (NonEmptyQueue<T>)[output.tail, input]]
                else {
                    FList<T> newOut = input.reverse(FList.emptyList)
                    [output.head, (NonEmptyQueue<T>)[newOut, FList.emptyList]]
                }
            }
        }

        Iterator<T> iterator () {
            output.iterator() | input.reverse(FList.emptyList).iterator()
        }

        String toString () {
            "[$output,$input]"
        }

        final int size () {
            input.size + output.size
        }
    }
}