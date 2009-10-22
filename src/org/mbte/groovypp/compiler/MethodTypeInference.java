package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GenericsType;
import static org.mbte.groovypp.compiler.MethodTypeInference.Constraint.*;
import static org.mbte.groovypp.compiler.TypeUtil.*;

/**
 * @author ven
 */
public class MethodTypeInference {
    static enum Constraint {
        EQ, SUBTYPE, SUPERTYPE
    }

    private class Constraints {
        ClassNode eqType;
        ClassNode superType;
        ClassNode subType;

        boolean contradictory = false;

        void addConstraint(ClassNode type, Constraint constraint) {
            switch (constraint) {
                case EQ:
                    if (eqType != null && !equal(eqType, type)) {
                        contradictory = true;
                    }
                    eqType = type;
                    break;
                case SUBTYPE:
                    superType = superType == null ? type : commonType(superType, type);
                    break;
                case SUPERTYPE:
                    subType = type;  // todo
                    break;
            }
        }

        public boolean isContradictory() {
            return contradictory;
        }

        ClassNode obtainFinalType() {
            if (eqType != null) return eqType;
            else if (superType != null) return superType;
            else return subType;
        }
    }

    ClassNode[] inferTypeArguments(GenericsType[] typeVars,
                                   ClassNode[] formals,
                                   ClassNode[] instantiateds) {
        ClassNode[] result = new ClassNode[typeVars.length];
        NextVar:
        for (int i = 0; i < typeVars.length; i++) {
            GenericsType typeVar = typeVars[i];
            String name = typeVar.getType().getUnresolvedName();
            Constraints constraints = new Constraints();
            for (int j = 0; j < Math.min(formals.length, instantiateds.length); ++j) {
                // this is just for parameters, if we ever decide to infer from context, the variance will change
                ClassNode formal = mapTypeFromSuper(formals[j], formals[i].redirect(),
                        instantiateds[i].redirect());
                if (formal == null) continue;

                ClassNode instantiated = instantiateds[j];
                match(formal, instantiated, name, constraints, SUBTYPE);
                if (constraints.isContradictory()) return null;   // todo per var contradiction?
            }
            result[i] = constraints.obtainFinalType();
        }
        return result;
    }

    private void match(ClassNode formal, ClassNode instantiated, String name, Constraints constraints, Constraint ifToplevel) {
        if (name.equals(formal.getUnresolvedName())) constraints.addConstraint(instantiated, ifToplevel);
        if (!formal.redirect().equals(instantiated.redirect()) ||
                formal.getGenericsTypes().length != instantiated.getGenericsTypes().length) return;
        for (int i = 0; i < formal.getGenericsTypes().length; i++) {
            GenericsType fTypearg = formal.getGenericsTypes()[i];
            GenericsType iTypearg = instantiated.getGenericsTypes()[i];
            ClassNode fType = fTypearg.getType();
            ClassNode iType = iTypearg.getType();
            if (isSuper(fTypearg)) {
                if (iTypearg.isWildcard()) continue;
                fType = mapTypeFromSuper(fType, iType.redirect(), fType.redirect());
                if (fType == null) continue;
                match(fType, iType, name, constraints, SUBTYPE);
            } else if (isExtends(fTypearg)) {
                if (iTypearg.isWildcard()) continue;
                iType = mapTypeFromSuper(iType, fType.redirect(), iType.redirect());
                if (iType == null) continue;
                match(fType, iType, name, constraints, SUPERTYPE);
            } else {
                if (iTypearg.isWildcard()) continue;
                match(fType, iType, name, constraints, EQ);
            }
        }
    }
}
