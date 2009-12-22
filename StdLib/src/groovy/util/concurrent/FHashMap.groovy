/**
 Copyright (c) 2007-2008, Rich Hickey
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above
 copyright notice, this list of conditions and the following
 disclaimer in the documentation and/or other materials provided
 with the distribution.

 * Neither the name of Clojure nor the names of its contributors
 may be used to endorse or promote products derived from this
 software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 * */

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
abstract class FHashMap<K, V> {
    abstract int size()

    abstract V getAt(K key, int hash)

    abstract FHashMap<K,V> update(int shift, K key, int hash, V value)

    abstract FHashMap<K,V> remove(K key, int hash)

    final V getAt(K key) { getAt(key, shuffle(key.hashCode())) }

    final FHashMap<K, V> put(K key, V value) {
        update(0, key, shuffle(key.hashCode()), value)
    }

    final FHashMap<K, V> remove(K key) {
        remove(key, shuffle(key.hashCode()))
    }

    static final EmptyNode emptyMap = []

    private static int shuffle(int h) {
        h += ~(h << 9)
        h ^= (h >>> 14)
        h += (h << 4)
        h ^= (h >>> 10)
        h
    }

    private static class EmptyNode<K,V> extends FHashMap<K,V> {
        private EmptyNode() {}

        int size() { 0 }

        V getAt(K key, int hash) { null }

        FHashMap<K,V> update(int shift, K key, int hash, V value) { new LeafNode(hash, key, value) }

        FHashMap<K,V> remove(K key, int hash) { this }
    }

    private static abstract class SingleNode<K,V> extends FHashMap<K,V> {
        int hash

        BitmappedNode<K,V> bitmapped(int shift, int hash, K key, V value) {
            def shift1 = (getHash() >>> shift) & 0x1f
            def shift2 = (hash >>> shift) & 0x1f
            def table = new FHashMap<K,V>[Math.max(shift1, shift2) + 1]
            table[shift1] = this
            def bits1 = 1 << shift1                                 
            def bits2 = 1 << shift2
            if (shift1 == shift2) {
                table[shift2] = table[shift2].update(shift + 5, key, hash, value)
            } else {
                table[shift2] = new LeafNode<K,V>(hash, key, value)
            }
            return new BitmappedNode<K,V>(shift, bits1 | bits2, table)
        }
    }

    private static class LeafNode<K,V> extends SingleNode<K,V> {
        K key
        V value

        def LeafNode(int hash, K key, V value) {
            this.hash = hash;
            this.key = key;
            this.value = value;
        }

        int size() { 1 }

        V getAt(K key, int hash) {
            if (this.key == key) return value else return null
        }

        FHashMap<K,V> update(int shift, K key, int hash, V value) {
            if (this.key == key) {
                if (this.value == value) return this else return new LeafNode(hash, key, value)
            } else if (this.hash == hash) {
                return new CollisionNode(hash, FList.emptyList + [this.key, this.value] + [key, value])
            } else {
                return bitmapped(shift, hash, key, value)
            }
        }

        FHashMap<K,V> remove(K key, int hash) {
            if (this.key == key) return emptyMap else return this
        }
    }

    private static class CollisionNode<K,V> extends SingleNode<K,V> {
        FList<Pair<K, V>> bucket

        CollisionNode(int hash, FList<Pair<K, V>> bucket) {
            this.hash = hash;
            this.bucket = bucket
        }

        int size() { bucket.size }

        V getAt(K key, int hash) {
            if (hash == this.hash) {
                def p = bucket.find { it.first.equals(key) }
                return p?.second
            }
        }

        private FList<Pair<K, V>> removeBinding(K key, FList<Pair<K, V>> bucket) {
            if (bucket.isEmpty()) return bucket
            if (bucket.getHead().first == key) return bucket.getTail()
            def t = removeBinding(key, bucket.getTail())
            if (t == bucket.getTail()) return bucket else return t + bucket.getHead()
        }

        FHashMap<K,V> update(int shift, K key, int hash, V value) {
            if (this.hash == hash) {
                return new CollisionNode(hash, removeBinding(key, bucket) + [key, value]);
            } else {
                return bitmapped(shift, hash, key, value)
            }
        }

        FHashMap<K,V> remove(K key, int hash) {
            if (hash != this.hash) return this
            def b = removeBinding(key, bucket)
            if (b == this) return this
            if (b.size == 1) {
                return new LeafNode<K,V>(hash, b.getHead().first, b.getHead().second)
            }
            new CollisionNode<K,V>(hash, b)
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

        int size() {
            table.filter {it != null}.foldLeft(0) {e, sum -> sum + e.size()}
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
                    return new FullNode<K,V>(shift, newTable)
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

    private static class FullNode extends FHashMap<K,V> {
        int shift
        FHashMap<K,V>[] table

        def FullNode(int shift, FHashMap<K,V>[] table) {
            this.shift = shift
            this.table = table
        }

        int size() {
            table.foldLeft(0) {e, sum -> sum + e.size() }
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
                return new FullNode<K,V>(shift, newTable)
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
                    return new BitmappedNode<K,V>(shift, ~mask, newTable)
                } else {
                    newTable[i] = node
                    return new FullNode<K,V>(shift, newTable)
                }
            }
        }
    }
}
