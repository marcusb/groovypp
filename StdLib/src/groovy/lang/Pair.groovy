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

@Typed
class Pair<T1, T2> implements Externalizable {
  T1 first
  T2 second

  Pair () {}  

  Pair(T1 first, T2 second) {
    this.first = first;
    this.second = second
  }

  boolean equals(obj) {
    this.is(obj) || (obj instanceof Pair && eq(first, ((Pair) obj).first) && eq(second, ((Pair) obj).second))
  }

  private boolean eq(obj1, obj2) {
    obj1 == null ? obj2 == null : obj1.equals(obj2)
  }

  int hashCode() {
    31 * first?.hashCode () + second?.hashCode ()
  }

  String toString() {
      "[first: $first, second: $second]"
  }
}