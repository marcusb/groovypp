package groovy.util

@Typed
abstract class MappingGenerator<T,R> implements Iterator<R> {
    Iterator<T> source

    private final LinkedList<R> generated = []

    MappingGenerator (Iterator<T> source) {
        this.source = source
    }

    private boolean stopped

    boolean hasNext() {
        !generated.empty || generate()
    }

    final R next() {
        generated.removeFirst()
    }

    final void remove() {
        throw new UnsupportedOperationException()
    }

    private boolean generate () {
        while (source.hasNext()) {
            tryGenerate(source.next())
            if(!generated.empty)
                return true
        }
        false
    }

    protected abstract void tryGenerate (T element)

    final MappingGenerator yield (R element) {
        generated << element
        this
    }
}