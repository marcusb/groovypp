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

import java.util.concurrent.atomic.AtomicInteger

class AtomicIntegerMap<K> extends AtomicMap<K,AtomicIntegerMap.Entry<K>> {

    static class Entry<K> extends AtomicInteger implements AtomicMapEntry<K,AtomicInteger> {}

    Entry<K> createEntry(K key, int hash) {
        [key:key, hash:hash]
    }
}