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

package groovy.lang

import java.util.concurrent.Callable
import java.util.concurrent.FutureTask

@Trait
abstract class Function0<R> implements Callable<R> {

    abstract R call ()

    public <R1> Function0<R1> andThen (Function1<R,R1> g) {
        { -> g.call(call()) }
    }

    FutureTask<R> future () {
        [this]
    }
}