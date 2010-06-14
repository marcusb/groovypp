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

class SerialInputStream extends ObjectInputStream {

    private final ClassLoader classLoader;

    SerialInputStream(InputStream inp, ClassLoader classLoader) throws IOException {
        super(inp);
        this.classLoader = classLoader;
    }

    @Override
    protected void readStreamHeader() throws IOException, StreamCorruptedException {
        int version = readByte() & 0xFF
        if (version != STREAM_VERSION) {
            throw new StreamCorruptedException("Unsupported version: $version")
        }
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        int type = read()
        if (type < 0) {
            throw new EOFException()
        }
        switch (type) {
        case 0:
            return super.readClassDescriptor()
        case 1:
            String className = readUTF()
            Class<?> clazz = loadClass(className)
            return ObjectStreamClass.lookup(clazz)
        default:
            throw new StreamCorruptedException("Unexpected class descriptor type: $type")
        }
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String className = desc.getName()
        try {
            return loadClass(className)
        } catch (ClassNotFoundException ex) {
            return super.resolveClass(desc)
        }
    }

    protected Class<?> loadClass(String className) throws ClassNotFoundException {
        Class<?> clazz
        ClassLoader classLoader = this.classLoader
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader()
        }

        if (classLoader != null) {
            clazz = classLoader.loadClass(className)
        } else {
            clazz = Class.forName(className)
        }
        return clazz
    }
}
