package groovy.util.concurrent

@Typed
abstract class FQueue<T> implements Iterable<T> {
    abstract boolean isEmpty ()

    T getFirst () { throw new NoSuchElementException() }

    Pair<T, FQueue<T>> removeFirst() { throw new NoSuchElementException() }

    abstract FQueue<T> addLast (T element)

    abstract FQueue<T> addFirst (T element)

    FQueue<T> plus (T element) {
        addLast(element)
    }

    /**
     * Number of elements in the list
     */
    static final EmptyQueue emptyQueue = []

    abstract int size ()

    private static final class EmptyQueue<T> extends FQueue<T> {
        EmptyQueue(){
        }

        OneElementQueue<T> addLast (T element)  { [element] }

        OneElementQueue<T> addFirst (T element) { [element] }

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

        boolean isEmpty () { true }
    }

    private static final class OneElementQueue<T> extends FQueue<T> {
        T head

        OneElementQueue(T head){
            this.head = head
        }

        MoreThanOneElementQueue<T> addLast (T element)  { [(FList.emptyList + element) + head, FList.emptyList] }

        MoreThanOneElementQueue<T> addFirst (T element) { [(FList.emptyList + head) + element, FList.emptyList] }

        T getFirst () { head }

        Pair<T, FQueue<T>> removeFirst() {
            [head, FQueue.emptyQueue]
        }

        Iterator<T> iterator () {
            head.singleton().iterator()
        }

        String toString () {
            "[$head]".toString()
        }

        final int size () { 1 }

        boolean isEmpty () { false }
    }

    private static final class MoreThanOneElementQueue<T> extends FQueue<T> {
        private final FList<T> input, output

        MoreThanOneElementQueue (FList<T> output, FList<T> input) {
            this.input  = input
            this.output = output
        }

        MoreThanOneElementQueue<T> addLast (T element) {
            [output, input + element]
        }

        MoreThanOneElementQueue<T> addFirst (T element) {
            [output + element, input]
        }

        T getFirst () { output.head }

        Pair<T, FQueue<T>> removeFirst() {
            if (size () == 2)
                [output.head, new OneElementQueue(output.tail.head)]
            else {
                if(output.size > 2)
                    [output.head, (MoreThanOneElementQueue<T>)[output.tail, input]]
                else {
                    [output.head, (MoreThanOneElementQueue<T>)[input.reverse() + output.tail.head, FList.emptyList]]
                }
            }
        }

        Iterator<T> iterator () {
            output.iterator() | input.reverse().iterator()
        }

        String toString () {
            "[$output,$input]"
        }

        final int size () {
            input.size + output.size
        }

        boolean isEmpty () { false }
    }
}