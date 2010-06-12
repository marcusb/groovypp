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

@Typed class FVectorSerialTest extends FSerialTestCase {

    void testEmpty() {
        def res = fromBytes(toBytes(FVector.emptyVector))
        assert res instanceof FVector
        FVector r = res
        assert r.length == 0
        assert FVector.emptyVector === r
    }

    void testSeveralEl() {
        def res = fromBytes(toBytes(FVector.emptyVector + "12" + "34" + "56"))
        assert res instanceof FVector
        FVector r = res
        assert r.length == 3
        assert r[0] == "12"
        assert r[1] == "34"
        assert r[2] == "56"
    }
}