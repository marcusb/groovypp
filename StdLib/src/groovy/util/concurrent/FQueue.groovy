package groovy.util.concurrent

@Typed
abstract class FQueue<T> {
    final boolean isEmpty () { size == 0 }

    T getFirst () { throw new NoSuchElementException() }

    FQueue<T> removeFirst() { throw new NoSuchElementException() }

    abstract FQueue<T> addLast (T element)

    abstract FQueue<T> addFirst (T element)

    /**
     * Number of elements in the list
     */
    final int size

    static final EmptyQueue emptyQueue = []

    FQueue (int size) {
        this.@size = size
    }

    private static class EmptyQueue<T> extends FQueue<T> {
        EmptyQueue(){
            super(0)
        }

        NonEmptyQueue<T> addLast (T element)  { [element, FList.emptyList, FList.emptyList] }

        NonEmptyQueue<T> addFirst (T element) { [element, FList.emptyList, FList.emptyList] }
    }

    private static class NonEmptyQueue<T> extends FQueue<T> {
        private final FList<T> input, output
        private final T head

        NonEmptyQueue (T head, FList<T> input, FList<T> output) {
            super(input.size + output.size + 1)
            this.@input  = input
            this.@output = output
            this.@head   = head
        }

        NonEmptyQueue<T> addLast (T element) {
            [head, output, input + element]
        }

        NonEmptyQueue<T> addFirst (T element) {
            [element, output + head, input]
        }

        T getFirst () { head }

        FQueue<T> removeFirst() {
            if (size == 1)
                FQueue.emptyQueue
            else {
                if(!output.empty)
                    (NonEmptyQueue<T>)[output.head, output.tail, input]
                else {
                    FList<T> newOut = input.reverse(FList.emptyList)
                    (NonEmptyQueue<T>)[newOut.head, newOut.tail, FList.emptyList]
                }
            }
        }
    }
}