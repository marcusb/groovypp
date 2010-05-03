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

package groovy.util.concurrent

import groovy.channels.ExecutingChannel

@Typed final class Agent<T> extends ExecutingChannel {
    private volatile T ref

    private volatile FList<Function1<Agent<T>,?>> listeners = FList.emptyList

    private volatile FList<Function2<Agent<T>,T,Boolean>> validators = FList.emptyList

    Agent (T ref = null, boolean runFair = false) {
      this.runFair = runFair
      this.ref = ref
    }

    final T get () { ref }

    void call (Mutation<T> mutation, Runnable whenDone = null) {
        def that = this
        schedule {
            def oldRef = ref
            def newRef = mutation(oldRef)
            if (newRef !== oldRef) {
                if(!validators.any{ !it(that, newRef) }) {
                    ref = newRef
                    listeners*.call(that)
                }
            }
            
            whenDone?.run ()
        }
    }

    void await (Mutation<T> mutation, Runnable whenDone = null) {
        CountDownLatch cdl = [1]
        call(mutation) {
            cdl.countDown ()
        }
        cdl.await()    
    }

    Function1<Agent<T>,?> addListener (Function1<Agent<T>,?> listener) {
        listeners.apply{ it + listener }
        listener
    }

    void removeListener (Function1<Agent<T>,?> listener) {
        listeners.apply{ it - listener }
    }

    Function2<Agent<T>,T,Boolean> addValidator (Function2<Agent<T>,T,Boolean> validator) {
        validators.apply{ it + validator }
        validator
    }

    void removeValidator (Function2<Agent<T>,T,Boolean> validator) {
        validators.apply{ it - validator }
    }
}
