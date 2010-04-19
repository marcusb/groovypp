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

/*
class SampleFStruct implements FStruct {
    private int balance

    SampleFStruct(SampleFStruct copyFrom) {
        this.balance = copyFrom.balance
    }

    int getBalance () {
        balance
    }

    Mutable makeMutable () {
        new Mutable(this)
    }

    Mutable withBalance(int balance) {
        new Mutable(this).withBalance(balance)
    }

    SampleFStruct mutate(Function1<Mutable,?> op) {
        def copy = makeMutable()
        op(copy)
        copy.makeImmutable()
    }

    static class Mutable extends SampleFStruct {
        Mutable (SampleFStruct copyFrom) {
            super(copyFrom)
        }

        Mutable withBalance(int balance) {
            this.balance = balance
            this
        }

        SampleFStruct makeImmutable () {
            new SampleFStruct(this)
        }
    }
}
*/