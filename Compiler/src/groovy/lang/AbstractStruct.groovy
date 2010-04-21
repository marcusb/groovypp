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

@Typed abstract class AbstractStruct implements Cloneable, Externalizable {
    static class Builder<T extends AbstractStruct>  {
        protected T obj

        protected Builder(T obj) {
            this.obj = obj
        }

        final T build () {
            def r = obj
            obj = null
            r
        }

        String toString() { obj.toString() }

        int hasCode () { obj.hashCode() }

        boolean equals (Object other) { obj.equals(other) }
    }

    def clone () {
        super.clone ()
    }

    String toString() {
        def sb = new StringBuilder()
        sb.append(this.class.simpleName.replace('\$','.'))
        sb.append("{")
        toString(sb)
        sb.append("}")
    }

    void toString(StringBuilder sb) {}

    abstract static class ApplyOp<B extends Builder> implements Delegating<B> {
        abstract void call ()
    }
}
