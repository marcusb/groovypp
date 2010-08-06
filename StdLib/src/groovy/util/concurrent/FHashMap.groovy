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
 * A clean-room port of Rich Hickey's persistent hash trie implementation from
 * Clojure (http://clojure.org).  Originally presented as a mutable structure in
 * a paper by Phil Bagwell.
 *
 * @author Daniel Spiewak
 * @author Rich Hickey
 * @author Alex Tkachman
 */

@Typed
abstract class FHashMap<K, V> implements Iterable<Map.Entry<K,V>>, Serializable {
    final int size() {
      if (size < 0) size = size_()
      size
    }

    final V getAt(K key) { getAt(key, hash(key)) }

    private static int hash(K key) {
        def h = key.hashCode()
        h += ~(h << 9)
        h ^=  (h >>> 14)
        h +=  (h << 4)
        h ^=  (h >>> 10)
        h
    }

    final FHashMap<K, V> put(K key, V value) {
        update(0, key, hash(key), value)
    }

    final FHashMap<K, V> remove(K key) {
        remove(key, hash(key))
    }

    private int size = -1

    protected abstract int size_()

    protected abstract V getAt(K key, int hash)

    protected abstract FHashMap<K,V> update(int shift, K key, int hash, V value)

    protected abstract FHashMap<K,V> remove(K key, int hash)

    static final FHashMap emptyMap = new EmptyNode()

    protected final Object writeReplace() {
        new Serial(fmap:this)
    }

    static class Serial implements Externalizable {
        FHashMap fmap

        protected final Object readResolve() {
            fmap
        }

        void writeExternal(ObjectOutput out) {
            out.writeInt fmap.size()
            for(e in fmap) {
                out.writeObject e.key
                out.writeObject e.value
            }
        }

        void readExternal(ObjectInput input) {
            def sz = input.readInt()
            def res = FHashMap.emptyMap
            while(sz--) {
                res = res.put(input.readObject(), input.readObject())
            }
            fmap = res
        }
    }

    private static class EmptyNode<K,V> extends FHashMap<K,V> {
        private EmptyNode() {}

        int size_() { 0 }

        V getAt(K key, int hash) { null }

        FHashMap<K,V> update(int shift, K key, int hash, V value) { new LeafNode(hash, key, value) }

        FHashMap<K,V> remove(K key, int hash) { this }

        Iterator iterator () {
            [
                hasNext:{false},
                next:{throw new NoSuchElementException()},
                remove:{throw new UnsupportedOperationException()}
            ]
        }
    }

    protected static int bitIndex(int bit, int mask) {
       Integer.bitCount(mask & (bit - 1))
    }

    private static abstract class SingleNode<K,V> extends FHashMap<K,V> {
        final int hash

        SingleNode(int hash) {
          this.hash = hash
        }

        BitmappedNode bitmap(int shift, int hash, K key, V value) {
            def shift1 = (getHash() >>> shift) & 0x1f
            def shift2 = (hash >>> shift) & 0x1f

            def bits1 = 1 << shift1
            def bits2 = 1 << shift2

            def mask = bits1 | bits2

            shift1 = bitIndex(bits1, mask)
            shift2 = bitIndex(bits2, mask)

            def table = new FHashMap<K,V>[shift1 == shift2 ? 1 : 2]
            table[shift1] = this
            if (shift1 == shift2) {
                table[shift2] = table[shift2].update(shift + 5, key, hash, value)
            } else {
                table[shift2] = new LeafNode(hash, key, value)
            }
            return new BitmappedNode(shift, mask, table)
        }
    }

    private static class LeafNode<K,V> extends SingleNode<K,V> implements Map.Entry<K,V> {
        final K key
        final V value

        LeafNode(int hash, K key, V value) {
            super(hash)
            this.@key = key  // todo remove '@'
            this.@value = value
        }

        int size_() { 1 }

        V getAt(K key, int hash) {
            if (this.key == key) return value else return null
        }

        FHashMap<K,V> update(int shift, K key, int hash, V value) {
            if (this.key == key) {
                if (this.value == value) return this else return new LeafNode(hash, key, value)
            } else if (this.hash == hash) {
                return new CollisionNode(hash, FList.emptyList + new BucketElement(this.key, this.value) + new BucketElement(key, value))
            } else {
                return bitmap(shift, hash, key, value)
            }
        }

        FHashMap<K,V> remove(K key, int hash) {
            if (this.key == key) return emptyMap else return this
        }

        Iterator iterator () {
            [
                _hasNext:true,
                hasNext:{_hasNext},
                next:{if(_hasNext) {_hasNext = false; LeafNode.this } else {throw new NoSuchElementException()} },
                remove:{throw new UnsupportedOperationException()}
            ]
        }

        public Object setValue(Object value) {
            throw new UnsupportedOperationException()
        }
    }

    private static class BucketElement<K,V> implements Map.Entry<K,V> {
        final K key
        final V value

        BucketElement(K key, V value) {
            this.key = key
            this.value = value
        }

        public V setValue(V value) {
            throw new UnsupportedOperationException()
        }
    }

    private static class CollisionNode<K,V> extends SingleNode<K,V> {
        FList<BucketElement<K, V>> bucket

        CollisionNode(int hash, FList<BucketElement<K, V>> bucket) {
            super(hash)
            this.bucket = bucket
        }

        int size_() { bucket.size }

        V getAt(K key, int hash) {
            if (hash == this.hash) {
                def p = bucket.find { it.key.equals(key) }
                return p?.value
            }
        }

        public Iterator<Map.Entry<K, V>> iterator() {
            bucket.iterator()
        }

        private FList<BucketElement<K, V>> removeBinding(K key, FList<BucketElement<K, V>> bucket) {
            if (bucket.isEmpty()) return bucket
            if (bucket.head.key == key) return bucket.tail
            def t = removeBinding(key, bucket.tail)
            t === bucket.tail ? bucket : t + bucket.head
        }

        FHashMap<K,V> update(int shift, K key, int hash, V value) {
            if (this.hash == hash) {
                return new CollisionNode(hash, removeBinding(key, bucket) + [key, value]);
            } else {
                return bitmap(shift, hash, key, value)
            }
        }

        FHashMap<K,V> remove(K key, int hash) {
            if (hash != this.hash) return this
            def b = removeBinding(key, bucket)
            if (b === this) return this
            if (b.size == 1) {
                return new LeafNode(hash, b.head.key, b.head.value)
            }
            new CollisionNode(hash, b)
        }
    }

    private static class BitmappedNode<K,V> extends FHashMap<K,V> {
        int shift
        int bits
        FHashMap<K,V> [] table

        BitmappedNode(int shift, int bits, FHashMap<K,V>[] table) {
            this.shift = shift;
            this.bits = bits;
            this.table = table;
        }

        int size_() {
            table.filter {it != null}.foldLeft(0) {e, sum -> sum + e.size()}
        }

        public Iterator<Map.Entry<K, V>> iterator() {
            table.iterator().filter {it != null}.map{it.iterator()}.flatten()
        }

        V getAt(K key, int hash) {
            def i = (hash >>> shift) & 0x1f
            def bit = 1 << i
            bits & bit ? table[bitIndex(bit, bits)].getAt(key, hash) : null
        }

        FHashMap<K,V> update(int shift, K key, int hash, V value) {
            int i = (hash >>> shift) & 0x1f
            int bit = 1 << i
            if (bits & bit) {
                i = bitIndex(bit, bits)
                def node = table[i].update(shift + 5, key, hash, value)
                if (node === table[i])
                    return this
                else {
                    FHashMap<K,V>[] newTable = table.clone()
                    newTable[i] = node
                    return new BitmappedNode(shift, bits, newTable)
                }
            } else {
                def newBits = bits | bit
                i = bitIndex(bit, newBits)

                def newTable = new FHashMap<K,V>[table.length+1]
                if(i > 0)
                    System.arraycopy table, 0, newTable, 0, i
                newTable[i] = new LeafNode(hash, key, value)
                if(i < table.length)
                    System.arraycopy table, i, newTable, i+1, table.length-i

                return new BitmappedNode(shift, newBits, newTable)
            }
        }

        FHashMap<K,V> remove(K key, int hash) {
            int i = (hash >>> shift) & 0x1f
            int bit = 1 << i
            if (bits & bit) {
                i = bitIndex(bit, bits)
                def node = table[i].remove(key, hash)
                if (node === table[i]) {
                    return this
                } else if (node === emptyMap) {
                    int adjustedBits = bits & ~bit
                    if (!adjustedBits)
                        return emptyMap
                    if (!(adjustedBits & (adjustedBits - 1))) {
                        // Last one.
                        return table[bitIndex(adjustedBits,bits)]
                    }
                    def newTable = new FHashMap<K,V>[table.length-1]
                    if(i>0)
                        System.arraycopy table, 0, newTable, 0, i
                    if(i<table.length-1)
                        System.arraycopy table, i+1, newTable, i, table.length-1-i
                    return new BitmappedNode(shift, adjustedBits, newTable)
                } else {
                    FHashMap<K,V>[] newTable = table.clone()
                    newTable[i] = node
                    return new BitmappedNode(shift, bits, newTable)
                }
            }
            else {
                return this
            }
        }
    }
}
