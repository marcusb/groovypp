package org.mbte.groovypp.compiler.types;

import java.util.Map;

/**
 * @author ven
 */
public interface Substitutor {
    public Type substitute(Type t);

    Substitutor EMPTY = new Substitutor() {
        public Type substitute(Type t) {
            return t;
        }
    };
}
