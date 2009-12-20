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
class HashTrie<K,V> {
  Node<K,V> root
  HashTrie() { root = new EmptyNode() }
  HashTrie(Node<K,V> root) { this.@root = root }

  int size() { root.size() }
  V getAt(K key) { root[key] }

  HashTrie<K,V> put(K key, V value) {
    new HashTrie(root.update(0, key, key.hashCode(), value))
  }

  HashTrie<K,V> remove(K key) {
    new HashTrie(root.remove(key, key.hashCode()))
  }

  // TODO: interface
  abstract class Node<K,V> {
    abstract int size()
    abstract V getAt(K key)
    abstract Node<K,V> update(int shift, K key, int hash, V value)
    abstract Node<K,V> remove(K key, int hash)
  }

  class EmptyNode extends Node<K, V> {
    int size() { 0 }
    V getAt(K key) { null }
    Node<K,V> update(int shift, K key, int hash, V value) { new LeafNode(key, hash, value) }
    Node<K,V> remove(K key, int hash) { this }
  }

  abstract class SingleNode extends Node<K,V> {
    abstract int getHash()
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

    V getAt(K key) {
      if (this.key == key) return value else return null
    }

    Node<K, V> update(int shift, K key, int hash, V value) {
      if (this.key == key) {
        if (this.value == value) return this else return new LeafNode(key, hash, value)
      } else if (this.hash == hash) {
        return new CollisionNode(hash, [this.key, this.value], [key, value])
      } else {
        return /*new BitmappedNode()*/ this
      }
    }

    Node<K, V> remove(K key, int hash) {
      if (this.key == key) return new EmptyNode() else return this
    }
  }

  class CollisionNode extends SingleNode {
    int hash
    Pair<K,V>[] bucket

    def CollisionNode(int hash, Pair<K,V>... bucket) {
      this.hash = hash;
      this.bucket = bucket
    }

    int size() { bucket.length }

    V getAt(K key) {
      if (key.hashCode() == hash) {
        def p = bucket.find { it.first.equals(key) }
        return p?.second
      }
    }

    Node<K, V> update(int shift, K key, int hash, V value) {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    Node<K, V> remove(K key, int hash) {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
  }
}
