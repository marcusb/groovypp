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
 **/

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
class FHashMap<K,V> {
  private Node root
  FHashMap() { root = EmptyNode.INSTANCE }
  FHashMap(Node root) { this.@root = root }

  int size() { root.size() }
  V getAt(K key) { root.getAt(key, shuffle(key.hashCode())) }

  FHashMap<K,V> put(K key, V value) {
    new FHashMap(root.update(0, key, shuffle(key.hashCode()), value))
  }

  FHashMap<K,V> remove(K key) {
    new FHashMap(root.remove(key, shuffle(key.hashCode())))
  }

  static int shuffle(int h) {
    h += ~(h << 9)
    h ^= (h >>> 14)
    h += (h << 4)
    h ^= (h >>> 10)
    h
  }

  // TODO: interface
  abstract class Node {
    abstract int size()
    abstract V getAt(K key, int hash)
    abstract Node update(int shift, K key, int hash, V value)
    abstract Node remove(K key, int hash)
  }

  class EmptyNode extends Node {
    private EmptyNode() {}

    int size() { 0 }
    V getAt(K key, int hash) { null }
    Node update(int shift, K key, int hash, V value) { new LeafNode(key, hash, value) }
    Node remove(K key, int hash) { this }

    static EmptyNode INSTANCE = new EmptyNode()
  }

  abstract class SingleNode extends Node {
    abstract int getHash()
    BitmappedNode bitmapped(int shift, int hash, K key, V value) {
      def shift1 = getHash() >>> shift
      def shift2 = hash >>> shift
      def table = new Node[Math.max(shift1, shift2) + 1]
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

  class LeafNode extends SingleNode {
    int hash
    K key
    V value

    def LeafNode(hash, key, value) {
      this.hash = hash;
      this.key = key;
      this.value = value;
    }

    int size() { 1 }

    V getAt(K key, int hash) {
      if (this.key == key) return value else return null
    }

    Node update(int shift, K key, int hash, V value) {
      if (this.key == key) {
        if (this.value == value) return this else return new LeafNode(key, hash, value)
      } else if (this.hash == hash) {
        return new CollisionNode(hash, FList.emptyList + [this.key, this.value] + [key, value])
      } else {
        return bitmapped(shift, hash, key, value)
      }
    }

    Node remove(K key, int hash) {
      if (this.key == key) return EmptyNode.INSTANCE else return this
    }
  }

  class CollisionNode extends SingleNode {
    int hash
    FList<Pair<K,V>> bucket

    CollisionNode(int hash, FList<Pair<K,V>> bucket) {
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

    private FList<Pair<K,V>> removeBinding(K key, FList<Pair<K,V>> bucket) {
      if (bucket.isEmpty()) return bucket
      if (bucket.getHead().first == key) return bucket.getTail()
      def t = removeBinding(key, bucket.getTail())
      if (t == bucket.getTail()) return bucket else return t + bucket.getHead()
    }

    Node update(int shift, K key, int hash, V value) {
      if (this.hash == hash) {
        return new CollisionNode(hash, removeBinding(key, bucket) + [key, value]);
      } else {
        return bitmapped(shift, hash, key, value)
      }
    }

    Node remove(K key, int hash) {
      if (hash != this.hash) return this
      def b = removeBinding(key, bucket)
      if (b == this) return this
      if (b.size == 1) {
        return new LeafNode(hash, b.getHead().first, b.getHead().second)
      }
      new CollisionNode(hash, b)
    }
  }

  class BitmappedNode extends Node {
    int shift
    int bits
    Node[] table

    def BitmappedNode(int shift, int bits, Node<K,V>[] table) {
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

    Node update(int shift, K key, int hash, V value) {
      def i = (hash >>> shift) & 0x1f
      def mask = 1 << i
      if (bits & mask) {
        def node = table[i].update(shift + 5, key, hash, value)
        if (node == table[i]) return this else {
          def newTable = new Node[table.length]
          System.arraycopy table, 0, newTable, 0, table.length
          newTable[i] = node
          return new BitmappedNode(shift, bits, newTable)
        }
      } else {
        def newTable = new Node[Math.max(table.length, i + 1)]
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

    Node remove(K key, int hash) {
      def i = (hash >>> shift) & 0x1f
      def mask = 1 << i
      if (bits & mask) {
        def node = table[i].remove(key, hash)
        if (node == table[i]) {
          return this
        } else if (node == EmptyNode.INSTANCE) {
          def adjustedBits = bits & ~mask
          if (!adjustedBits) return EmptyNode.INSTANCE
          if (!(adjustedBits & (adjustedBits - 1))) {
            // Last one.
            for (j in 0..31) {
              if (adjustedBits == 1 << j) return table[j]
            }
          }
          def newTable = new Node[table.length]
          System.arraycopy table, 0, newTable, 0, table.length
          newTable[i] = null
          return new BitmappedNode(shift, adjustedBits, newTable)
        } else {
          def newTable = new Node[table.length]
          System.arraycopy table, 0, newTable, 0, table.length
          newTable[i] = node
          return new BitmappedNode(shift, bits, newTable)
        }
      } else return this
    }
  }

  class FullNode extends Node {
    int shift
    Node[] table


    def FullNode(int shift, Node[] table) {
      this.shift = shift
      this.table = table
    }

    int size() {
      table.foldLeft(0) { e, sum -> sum + e.size() }
    }

    V getAt(K key, int hash) {
      table[(hash >>> shift) & 0x1f].getAt(key, hash)
    }

    Node update(int shift, K key, int hash, V value) {
      def i = (hash >>> shift) & 0x1f
      def node = table[i].update(shift + 5, key, hash, value)
      if (node == table[i]) return this else {
        def newTable = new Node[32]
        System.arraycopy table, 0, newTable, 0, 32
        newTable[i] = node
        return new FullNode(shift, newTable)
      }
    }

    Node remove(K key, int hash) {
      def i = (hash >>> shift) & 0x1f
      def node = table[i].remove(key, hash)
      if (node == table[i]) return this else {
        def newTable = new Node[32]
        System.arraycopy table, 0, newTable, 0, 32
        if (node == EmptyNode.INSTANCE) {
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
