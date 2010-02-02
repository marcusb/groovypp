package groovy.remote

import groovy.util.concurrent.*

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