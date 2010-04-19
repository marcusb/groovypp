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

@Typed class BytesCharSequence implements CharSequence {

    private final byte [] b
    private final int start
    private final int end

    BytesCharSequence(byte [] b) {
        this(b, 0, b.length)
    }

    MetaClass getMetaClass () {}
    void setMetaClass (MetaClass mc) {}

    BytesCharSequence(byte [] b, int start, int end) {
        this.b = b
        this.start = start
        this.end = end
    }

    final int length() {
        end - start
    }

    char charAt(int index) {
        ((int)b [start + index]) & 0xff
    }

    CharSequence subSequence(int start, int end) {
        new BytesCharSequence(this.b, this.start + start, this.start + end)
    }

    String toString() {
        new String(b, 0, start, end-start)
    }
}
