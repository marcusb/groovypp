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

@Typed class Agent<T> extends FairExecutingChannel<Function1<T,T>> {
    private volatile T ref

    Agent () {}

    Agent (T ref) { this.@ref = ref }
    
    final T get () { ref }

    void call (Function1<T,T> mutation) {
        post mutation
    }

    void onMessage(Function1<T, T> message) {
        ref = message(ref)
    }
}
