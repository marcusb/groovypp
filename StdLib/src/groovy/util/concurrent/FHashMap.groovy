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
 */

@Typed
abstract class FHashMap<K, V> implements Iterable<Map.Entry<K,V>> {
    final int size() {
      if (size < 0) size = size_()
      size
    }

    final V getAt(K key) { getAt(key, key.hashCode()) }

    final FHashMap<K, V> put(K key, V value) {
        update(0, key, key.hashCode(), value)
    }

    final FHashMap<K, V> remove(K key) {
        remove(key, key.hashCode())
    }

    private int size = -1

    protected abstract int size_()

    protected abstract V getAt(K key, int hash)

    protected abstract FHashMap<K,V> update(int shift, K key, int hash, V value)

    protected abstract FHashMap<K,V> remove(K key, int hash)

    static final FHashMap emptyMap = new EmptyNode()

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

    private static abstract class SingleNode<K,V> extends FHashMap<K,V> {
        final int hash

        SingleNode(int hash) {
          this.hash = hash
        }

        BitmappedNode bitmap(int shift, int hash, K key, V value) {
            def shift1 = (getHash() >>> shift) & 0x1f
            def shift2 = (hash >>> shift) & 0x1f
            def table = new FHashMap<K,V>[Math.max(shift1, shift2) + 1]
            table[shift1] = this
            def bits1 = 1 << shift1                                 
            def bits2 = 1 << shift2
            if (shift1 == shift2) {
                table[shift2] = table[shift2].update(shift + 5, key, hash, value)
            } else {
                table[shift2] = new LeafNode(hash, key, value)
            }
            return new BitmappedNode(shift, bits1 | bits2, table)
        }
    }

    private static class LeafNode<K,V> extends SingleNode<K,V> implements Map.Entry<K,V> {
        final K key
        final V value

        def LeafNode(int hash, K key, V value) {
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
            if (t == bucket.getTail()) return bucket else return t + bucket.head
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
            if (b == this) return this
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

        def BitmappedNode(int shift, int bits, FHashMap<K,V>[] table) {
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
            def mask = 1 << i
            if (bits & mask) return table[i].getAt(key, hash)
        }

        FHashMap<K,V> update(int shift, K key, int hash, V value) {
            def i = (hash >>> shift) & 0x1f
            def mask = 1 << i
            if (bits & mask) {
                def node = table[i].update(shift + 5, key, hash, value)
                if (node == table[i]) return this else {
                    def newTable = new FHashMap<K,V>[table.length]
                    System.arraycopy table, 0, newTable, 0, table.length
                    newTable[i] = node
                    return new BitmappedNode(shift, bits, newTable)
                }
            } else {
                def newTable = new FHashMap<K,V>[Math.max(table.length, i + 1)]
                System.arraycopy table, 0, newTable, 0, table.length
                newTable[i] = new LeafNode(hash, key, value)
                def newBits = bits | mask
                if (newBits == ~0) {
                    return new FullNode(shift, newTable)
                } else {
                    return new BitmappedNode(shift, newBits, newTable)
                }
            }
        }

        FHashMap<K,V> remove(K key, int hash) {
            def i = (hash >>> shift) & 0x1f
            def mask = 1 << i
            if (bits & mask) {
                def node = table[i].remove(key, hash)
                if (node == table[i]) {
                    return this
                } else if (node == emptyMap) {
                    def adjustedBits = bits & ~mask
                    if (!adjustedBits) return emptyMap
                    if (!(adjustedBits & (adjustedBits - 1))) {
                        // Last one.
                        for (j in 0..31) {
                            if (adjustedBits == 1 << j) return table[j]
                        }
                    }
                    def newTable = new FHashMap<K,V>[table.length]
                    System.arraycopy table, 0, newTable, 0, table.length
                    newTable[i] = null
                    return new BitmappedNode(shift, adjustedBits, newTable)
                } else {
                    def newTable = new FHashMap<K,V>[table.length]
                    System.arraycopy table, 0, newTable, 0, table.length
                    newTable[i] = node
                    return new BitmappedNode(shift, bits, newTable)
                }
            } else return this
        }
    }

    private static class FullNode<K,V> extends FHashMap<K,V> {
        int shift
        FHashMap<K,V>[] table

        def FullNode(int shift, FHashMap<K,V>[] table) {
            this.shift = shift
            this.table = table
        }

        int size_() {
            table.foldLeft(0) {e, sum -> sum + e.size() }
        }

        public Iterator<Map.Entry<K, V>> iterator() {
            table.iterator().map{it.iterator()}.flatten()
        }

        V getAt(K key, int hash) {
            table[(hash >>> shift) & 0x1f].getAt(key, hash)
        }

        FHashMap<K,V> update(int shift, K key, int hash, V value) {
            def i = (hash >>> shift) & 0x1f
            def node = table[i].update(shift + 5, key, hash, value)
            if (node == table[i]) return this else {
                def newTable = new FHashMap<K,V>[32]
                System.arraycopy table, 0, newTable, 0, 32
                newTable[i] = node
                return new FullNode(shift, newTable)
            }
        }

        FHashMap<K,V> remove(K key, int hash) {
            def i = (hash >>> shift) & 0x1f
            def node = table[i].remove(key, hash)
            if (node == table[i]) return this else {
                def newTable = new FHashMap<K,V>[32]
                System.arraycopy table, 0, newTable, 0, 32
                if (node == emptyMap) {
                    newTable[i] = null
                    def mask = 1 << i
                    return new BitmappedNode(shift, ~mask, newTable)
                } else {
                    newTable[i] = node
                    return new FullNode(shift, newTable)
                }
            }
        }
    }
}
