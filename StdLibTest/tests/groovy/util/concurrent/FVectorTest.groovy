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

@Typed
public class FVectorTest extends GroovyTestCase {
    void testAddRemove() {
        FVector<Integer> vec = FVector.emptyVector

        for (i in 0..<1000000) {
            vec = vec + i
            if (i % 2) {
                def p = vec.pop()
                assertEquals i, p.first
                vec = p.second
            }
        }

        assertEquals 500000, vec.length

        for (i in 0..<500000) {
            assertEquals 2 * i, vec[i]
        }
    }

    void testIterator() {
        FVector<Integer> vec = FVector.emptyVector
        def range = 0..<100000
        for (i in range) {
            vec = vec + i
        }

        def l = []
        for (i in vec) {
            l << i
        }

        assertEquals range, l
    }

    void testShuffle() {
        FVector<Integer> vec = FVector.emptyVector

        for (i in 0..<10000) {
            vec = vec + i
        }

        def r = new Random()
        for (i in 0..<10000) {
            def i1 = r.nextInt(10000)
            def i2 = r.nextInt(10000)

            def v1 = vec[i1]
            def v2 = vec[i2]
            vec = vec.set(i2, v1)
            vec.set(i1, v2)
        }
    }
}