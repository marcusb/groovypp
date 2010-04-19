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

@Typed
abstract class MappingGenerator<T,R> implements Iterator<R> {
    Iterator<T> source

    private final LinkedList<R> generated = []

    MappingGenerator (Iterator<T> source) {
        this.source = source
    }

    private boolean stopped

    boolean hasNext() {
        !generated.empty || generate()
    }

    final R next() {
        generated.removeFirst()
    }

    final void remove() {
        throw new UnsupportedOperationException()
    }

    private boolean generate () {
        while (source.hasNext()) {
            tryGenerate(source.next())
            if(!generated.empty)
                return true
        }
        false
    }

    protected abstract void tryGenerate (T element)

    final MappingGenerator yield (R element) {
        generated << element
        this
    }
}