/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package groovy.util.concurrent

/**
 * Simple implementation of one-directional immutable functional list
 */
@Typed
abstract static class FList<T> implements Iterable<T>, Serializable {
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
     * Creates new list containing all elements of this list except given one
     */
    final FList<T> minus (T element) {
        remove(element)
    }

    /**
     * Creates new list containing all elements of this list except given one
     */
    final FList<T> remove (T element) {
        contains(element) ? doRemove(element, FList.emptyList) : this
    }

    /**
     * Creates new list containing all elements of this list except given one
     */
    private FList<T> doRemove (T element, FList<T> accumulated) {
        !size ?
            accumulated.reverse() :
            head == element ?
                tail.addAll(accumulated) :
                tail.doRemove(element,accumulated + head)
    }

    /**
     * Creates new list containing given element and then all element of this list
     */
    final FList<T> addAll (Iterable<T> elements) {
        def res = this
        for (el in elements)
            res += el
        res
    }

    /**
     * Utility method allowing convenient syntax <code>flist ()</code> for accessing head of the list
     */
    final T call () { head }

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

    protected final Object writeReplace() {
        new Serial(flist:this)
    }

    static class Serial implements Externalizable {
        FList flist

        protected final Object readResolve() {
            flist
        }

        void writeExternal(ObjectOutput out) {
            out.writeInt flist.size()
            for(e in flist) {
                out.writeObject(e)
            }
        }

        void readExternal(ObjectInput input) {
            int sz = input.readInt()
            if (!sz)
                flist = FList.emptyList
            else {
                if (sz == 1) {
                    flist = new OneElementList(input.readObject())
                }
                else {
                    MoreThanOneElementList res = [input.readObject(), sz-1], cur = res
                    sz--;
                    for( ; sz > 1; sz--) {
                        MoreThanOneElementList nextCur = [input.readObject(), sz-1]
                        cur.tail = nextCur
                        cur = nextCur
                    }
                    cur.tail = new OneElementList(input.readObject())
                    flist = res
                }
            }
        }
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

        protected OneElementList (T head, int addSize) {
            super(addSize+1)
            this.head = head
        }

        final MoreThanOneElementList<T> plus(T element) {
            [element, this]
        }

        public Iterator<T> iterator() {
            head.singleton().iterator()
        }

        public FList<T> getTail() {
            emptyList
        }

        String toString () { "[$head]" }
    }

    private static class MoreThanOneElementList<T> extends OneElementList<T> {
        FList<T> tail

        MoreThanOneElementList (T head, FList<T> tail) {
            super (head, tail.size)
            this.tail = tail
        }

        MoreThanOneElementList (T head, int size) {
            super (head, size)
        }

        Iterator<T> iterator () {
            [
                cur:     (FList<T>)this,
                hasNext: { cur.size },
                next:    {
                    def that = cur; 
                    cur = cur.tail;
                    that.head
                },
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
                if (tail instanceof MoreThanOneElementList)
                    ((MoreThanOneElementList)tail).toString(sb)
                else
                    sb << ((OneElementList)tail).head.toString()
            }
        }
    }
}
