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

class SerialOutputStream extends ObjectOutputStream {

    SerialOutputStream(OutputStream out) throws IOException {
        super(out)
    }

    @Override
    protected void writeStreamHeader() throws IOException {
        writeByte(STREAM_VERSION)
    }

    @Override
    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
        Class<?> clazz = desc.forClass()
        if (clazz.isPrimitive() || clazz.isArray()) {
            write(0)
            super.writeClassDescriptor(desc)
        } else {
            write(1)
            writeUTF(desc.name)
        }
    }
}
