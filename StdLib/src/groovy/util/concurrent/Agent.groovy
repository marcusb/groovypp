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

    abstract static class Validator<T> {
        abstract boolean validate (Agent<T> agent, T newValue)
    }

    private volatile FList<Listener<T>> listeners = FList.emptyList

    abstract static class Listener<T> {
        abstract void onUpdate (Agent<T> agent)
    }

    private volatile FList<Validator<T>> validators = FList.emptyList

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
                if(!validators.any{ !it.validate(that, newRef) }) {
                    ref = newRef
                    listeners*.onUpdate(that)
                }
            }
            
            whenDone?.run ()
        }
    }

    void await (Mutation<T> mutation) {
        CountDownLatch cdl = [1]
        call(mutation) {
            cdl.countDown ()
        }
        cdl.await()    
    }

    Listener<T> addListener (Listener<T> listener) {
        listeners.apply{ it + listener }
        listener
    }

    void removeListener (Listener<T> listener) {
        listeners.apply{ it - listener }
    }

    Validator<T> addValidator (Validator<T> validator) {
        validators.apply{ it + validator }
        validator
    }

    void removeValidator (Validator<T> validator) {
        validators.apply{ it - validator }
    }
}
