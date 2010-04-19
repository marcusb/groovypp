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

package groovy.lang;

import org.mbte.groovypp.runtime.HasDefaultImplementation;

/**
 * Marker interface for types which has not only own methods
 * but also able to delegate to other ones
 *
 * @param <D> type of delegate
 */
public interface Delegating<D> extends Cloneable {
    D getDelegate ();
    void setDelegate (D newDelegate);

    @HasDefaultImplementation(Delegating.TraitImpl.class)
    public Delegating<D> clone(D newDelegate) throws CloneNotSupportedException;

    public Object clone() throws CloneNotSupportedException;

    public static class TraitImpl {
        public static <D> Object clone(Delegating<D> self, D delegate) throws CloneNotSupportedException {
            Delegating<D> cloned = (Delegating<D>) self.clone();
            cloned.setDelegate(delegate);
            return cloned;
        }
    }
}
