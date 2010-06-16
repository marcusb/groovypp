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

package groovy.util

import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap

@Typed abstract class RefCleaner {
    static final protected ReferenceQueue queue = []

    static {
        Thread thread = [
            daemon: true,
            name: "RefCleaner handler",
            contextClassLoader: null,
            priority:Thread.MAX_PRIORITY,
            run: {
                java.lang.ref.Reference r
                for (;;) {
                    try {
                        r = queue.remove(1000)
                    }  catch (InterruptedException e) {
                        break
                    }

                    if (r) {
                        try {
                            switch(r) {
                                case RefCleanerRef:
                                        r.finalizeRef()
                                    break
                            }
                        }
                        finally {
                            r.clear()
                            r = null
                        }
                    }
                }
            }
        ]
        thread.start ()
    }
}

interface RefCleanerRef {
    void finalizeRef ()
}

@Trait abstract class MapValueRef<K,V> {
    protected RefValueMap<K,V,MapValueRef<K,V>> map
    protected K key

    void finalizeRef() {
        map.finalizeValueRef(this)
    }
}

abstract class RefValueMap<K,V,W extends MapValueRef<K,V>> extends ConcurrentHashMap<K,W> {
    boolean finalizeValueRef (W ref) {
        remove(ref.key, ref)
    }

    protected abstract W createValue(K key, V value)

    V putIfNotYet(K key, V value) {
        def newRef = createValue(key, value)
        while(true) {
            def prevRef = putIfAbsent(key, newRef)
            if (prevRef) {
                def prev = prevRef.get()
                if (prev)
                    return prev

                if (replace(key, prevRef, newRef))
                    return value
            }
            else {
                return value
            }
        }
    }
}

class WeakValueMap<K,V> extends RefValueMap<K,V,WeakValueMap.WeakValue<K,V>> {
    final static class WeakValue<K,V> extends WeakReference<V> implements MapValueRef<K,V> {
        WeakValue(WeakValueMap<K,V> map, K key, V value) {
            super(value, RefCleaner.queue)
            this.map = map
            this.key = key
        }
    }

    protected WeakValue<K,V> createValue (K key, V value) {
        [this, key, value]
    }
}

abstract class SoftValueMap<K,V> extends RefValueMap<K,V,SoftValue<K,V>> {
}

final class SoftValue<K,V> extends SoftReference<V> implements MapValueRef<K,V> {
    SoftValue(SoftValueMap<K,V> map, K key, V value) {
        super(value, RefCleaner.queue)
        this.map = map
        this.key = key
    }
}
