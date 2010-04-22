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

package groovy.remote

import groovy.util.concurrent.AtomicMap
import groovy.util.concurrent.AtomicMapEntry
import groovy.util.concurrent.Calculation

@Typed class RemoteClassLoader extends ClassLoader implements WithRemoteId {

    protected Cache resourceCache = [:]

    RemoteClassLoader(ClassLoader parent) {
        super(parent)
    }

    protected Class<?> findClass(String name) {
        try {
            def bytes = resourceCache [name.replace('.','/') + ".class"].get ()
            defineClass(name, bytes, 0, bytes.length)
        }
        catch (Throwable t) {
            throw new ClassNotFoundException(t.message, t)
        }
    }

    static class Cache extends AtomicMap<String,Cache.Entry> {
        Entry createEntry(String key, int hash) {
            [key:key, hash:hash]
        }

        static class Entry<K> extends Calculation<byte[]> implements AtomicMapEntry<String,Calculation<byte[]>> {
            void run () {
//                BindLater<byte[]> bytes = remote.getResourceBytes(key)
            }
        }
    }

//    static class Actor {
//        ClassLoader loader
//
//        @Async byte [] getResourceBytes(String key) {
//            def stream = loader.getResourceAsStream()
//
//        }
//    }
}