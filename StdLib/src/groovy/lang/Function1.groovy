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

import java.util.concurrent.FutureTask

@Trait
abstract class Function1<T,R> {

    abstract R call (T param)

    public <R1> Function1<T,R1> andThen (Function1<R,R1> g) {
        { arg -> g(call(arg)) }
    }

    public <T1> Function1<T1,R> composeWith (Function1<T1,T> g) {
        { arg -> call(g(arg)) }
    }

    R getAt (T arg) {
        call(arg)
    }

    Function0<R> curry (T arg) {
        { -> call(arg) }
    }

    FutureTask<R> future (T arg) {
        [ { -> call(arg) } ]
    }
}