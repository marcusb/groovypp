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

@Typed package groovy.util.concurrent

import groovy.util.concurrent.AtomicMapEntry
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class AtomicReferenceMap<K,V> extends AtomicMap<K,AtomicReferenceMap.Entry<K,V>> {

    static class Entry<K,V> extends AtomicReference<V> implements AtomicMapEntry<K,AtomicReference<V>> {}

    Entry<K,V> createEntry(K key, int hash) {
        [key:key, hash:hash]
    }
}