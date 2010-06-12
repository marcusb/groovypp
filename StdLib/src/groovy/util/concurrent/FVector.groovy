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

/*
  A straight port of Clojure's <code>PersistentVector</code> class.
  @author Daniel Spiewak
  @author Rich Hickey
*/
@Typed
class FVector<T> implements Iterable<T>, Serializable {
    int length
    int shift
    Object[] root
    T[] tail

    private def FVector(int length, int shift, Object[] root, T[] tail) {
        this.length = length
        this.shift = shift
        this.root = root
        this.tail = tail
    }

    static FVector<Object> emptyVector = new FVector(0, 5, new Object[0], new Object[0])

    private int tailOff() { length - tail.length }

    T getAt(int i) {
        if (i < 0)
            i += length

    if (i >= 0 && i < length) {
      if (i >= tailOff()) {
        return tail[i & 0x1f]
      } else {
        def arr = (shift..<0).step(5).foldLeft(root) {level, arr -> (Object[])arr[(i >>> level) & 0x1f] }
        return (T)arr[i & 0x1f]
      }
    } else {
      throw new IndexOutOfBoundsException("Tried to access FVector out of its bounds: " + i)
    }
  }

    FVector<T> set(int i, T obj) {
        if (i < 0)
            i += length

        if (i >= 0 && i < length) {
            if (i >= tailOff()) {
                def newTail = new T[tail.length]
                System.arraycopy tail, 0, newTail, 0, tail.length
        newTail[i - tailOff()] = obj
                return new FVector<T>(length, shift, root, newTail)
            } else {
                return new FVector<T>(length, shift, doAssoc(shift, root, i, obj), tail)
            }
        } else if (i == length) {
            return this + obj
        } else {
            throw new IndexOutOfBoundsException("Tried to update FVector out of its bounds: " + i)
        }
    }

    private Object[] doAssoc(int level, Object[] arr, int i, T obj) {
        def ret = new T[arr.length]
        System.arraycopy arr, 0, ret, 0, arr.length
        if (level == 0) {
            ret[i & 0x1f] = obj
        } else {
            def subi = (i >>> level) & 0x1f
            ret[subi] = doAssoc(level - 5, (Object[]) arr[subi], i, obj)
        }
        ret
    }

    FVector<T> addAll(Iterable<T> other) {
        other.foldLeft(this) {e, vec -> vec + e}
    }

    FVector<T> plus(T obj) {
        if (tail.length < 32) {
            def newTail = new T[tail.length + 1]
            System.arraycopy tail, 0, newTail, 0, tail.length
            newTail[tail.length] = obj
            return new FVector<T>(length + 1, shift, root, newTail)
        } else {
            def pushed = pushTail(shift - 5, root, tail)
            Object[] newRoot = pushed.first
            T expansion = pushed.second
            def newShift = shift
            if (expansion) {
                newShift += 5
                newRoot = [newRoot, expansion]
            }
            T[] newTail = [obj]
            return new FVector<T>(length + 1, newShift, newRoot, newTail)
        }
    }

    private Pair<Object[], Object> pushTail(int level, Object[] arr, T[] tailNode) {
        def newChild
        if (level == 0) newChild = tailNode else {
            def rec = pushTail(level - 5, (Object[]) arr[arr.length - 1], tailNode)
            def subexp = rec.second
            if (subexp != null) newChild = subexp else {
                def ret = new Object[arr.length]
                System.arraycopy arr, 0, ret, 0, arr.length
                ret[arr.length - 1] = rec.first
                return [ret, null]
            }
        }
        if (arr.length == 32) {
            return [arr, (Object[]) [newChild]]
        } else {
            def ret = new Object[arr.length + 1]
            System.arraycopy arr, 0, ret, 0, arr.length
            ret[arr.length] = newChild
            return [ret, null]
        }
    }

    Pair<T, FVector<T>> pop() {
        if (length == 0) {
            throw new IllegalStateException("Cannot pop from empty vector")
        } else if (length == 1) {
            return [tail[0], emptyVector]
        } else if (tail.length > 1) {
            def newTail = new T[tail.length - 1]
            System.arraycopy tail, 0, newTail, 0, newTail.length
            return [tail[tail.length - 1], new FVector<T>(length - 1, shift, root, newTail)]
        } else {
            def popped = popTail(shift - 5, root)
            def newRoot = popped.first
            def pTail = popped.second
            if (newRoot == null) newRoot = new Object[0]
            def newShift = shift
            if (shift > 5 && newRoot.length == 1) {
                newRoot = (Object[]) newRoot[0]
                newShift -= 5
            }
            return [tail[0], new FVector<T>(length - 1, newShift, newRoot, (T[]) pTail)]
        }
    }

    private Pair<Object[], Object> popTail(int shift, Object[] arr) {
        def newTail
        if (shift > 0) {
            def popped = popTail(shift - 5, (Object[]) arr[arr.length - 1])
            def newChild = popped.first
            def subPTail = popped.second
            if (newChild != null) {
                def ret = new Object[arr.length]
                System.arraycopy arr, 0, ret, 0, arr.length
                ret[arr.length - 1] = newChild
                return [ret, subPTail]
            }
            newTail = subPTail
        } else {
            newTail = arr[arr.length - 1]
        }
        if (arr.length == 1) {
            return [null, newTail]
        } else {
            def ret = new Object[arr.length - 1]
            System.arraycopy arr, 0, ret, 0, ret.length
            return [ret, newTail]
        }
    }

    Iterator<T> iterator() {
        (shift..<0).step(5).foldLeft(root.iterator()) { level, iter -> iter.map { ((Object[]) it).iterator() }.flatten() } |
                tail.iterator()
    }

    protected final Object writeReplace() {
        new Serial(fvector:this)
    }

    static class Serial implements Externalizable {
        FVector fvector

        protected final Object readResolve() {
            fvector
        }

        void writeExternal(ObjectOutput out) {
            out.writeInt fvector.length
            for(e in fvector) {
                out.writeObject e
            }
        }

        void readExternal(ObjectInput input) {
            def sz = input.readInt()
            def res = FVector.emptyVector
            while(sz--) {
                res += input.readObject()
            }
            fvector = res
        }
    }
}