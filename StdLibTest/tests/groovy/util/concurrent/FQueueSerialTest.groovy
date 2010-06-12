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

@Typed class FQueueSerialTest extends FSerialTestCase {

    void testEmpty() {
        def res = fromBytes(toBytes(FQueue.emptyQueue))
        assert res instanceof FQueue
        FQueue r = res
        assert r.size() == 0
        assert FQueue.emptyQueue === r
    }

    void testOneEl() {
        def res = fromBytes(toBytes(FQueue.emptyQueue + "one"))
        assert res instanceof FQueue
        FQueue r = res
        assert r.size() == 1
        assert r.first == "one"
    }

    void testSeveralEl() {
        def res = fromBytes(toBytes(FQueue.emptyQueue + "one" + "two" + "three"))
        assert res instanceof FQueue
        FQueue r = res
        assert r.size() == 3
        assert r.first == "one"
        r = r.removeFirst().second
        assert r.first == "two"
        r = r.removeFirst().second
        assert r.first == "three"
    }
}