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
public class FHashMapTest extends GroovyTestCase {
    void testInsert () {
        def map = FHashMap.emptyMap
        def m = map.put(10,-1).put(2,-2).put(2,-3)
        assertEquals 2, m.size()
        assertEquals (-3,m [2])
    }

    void testInsertMany () {
        Map<Integer,Integer> data = [:]
        def clock = System.currentTimeMillis()
        for(i in 0..<500000) {
            data[i] = -i
        }
        println("Map box & insert: ${System.currentTimeMillis()-clock}")

        FHashMap<Integer,Integer> map = FHashMap.emptyMap

        clock = System.currentTimeMillis()
        for(e in data.entrySet()) {
            map = map.put(e.key, e.value)
        }
        println("FMap insert: ${System.currentTimeMillis()-clock}")

        for(i in 0..<2500000) {
            map = map.remove(2*i)
        }
        assertEquals 250000, map.size()
        assertEquals (-25,map [25])
    }
}