package groovy.util

@Typed
class CompositeIterator<T> implements Iterator<T> {
    private final Iterator<T> [] iters
    private int   cur

    CompositeIterator(Iterator<T> [] iters) {
        this.@iters = iters
    }

    public boolean hasNext() {
        if(cur == -1)
            return false
        else {
            if (iters[cur].hasNext())
                return true
            else {
                if (++cur == iters.length) {
                    cur = -1
                    return false
                }
                return iters [cur].hasNext()
            }
        }
    }

    public T next() {
        iters[cur].next ()
    }

    public void remove() {
        iters[cur].remove()
    }
}