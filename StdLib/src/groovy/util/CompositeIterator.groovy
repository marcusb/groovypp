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
class CompositeIterator<T> implements Iterator<T> {
    private final Iterator<T> [] iters
    private int   cur

    CompositeIterator(Iterator<T> [] iters) {
        this.iters = iters
    }

    public boolean hasNext() {
        if(cur == -1)
            return false
        else {
            if (iters[cur].hasNext())
                return true
            else {
                if (++cur == iters.length) {
                    cur = -1
                    return false
                }
                return iters [cur].hasNext()
            }
        }
    }

    public T next() {
        iters[cur].next ()
    }

    public void remove() {
        iters[cur].remove()
    }
}