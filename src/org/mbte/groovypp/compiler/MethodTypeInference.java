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

    private static class Constraints {
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
            else return /*subType*/null;
        }
    }

    public static ClassNode[] inferTypeArguments(GenericsType[] typeVars,
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
                ClassNode instantiated = formals[j].isGenericsPlaceHolder() ? instantiateds[j] :
                        mapTypeFromSuper(formals[j].redirect(), formals[j].redirect(), instantiateds[j]);
                if (instantiated == null) continue;

                ClassNode formal = formals[j];
                match(formal, instantiated, name, constraints, SUBTYPE);
                if (constraints.isContradictory()) return null;   // todo per var contradiction?
            }
            result[i] = constraints.obtainFinalType();
            if (result[i] == null) return null;
        }
        return result;
    }

    private static void match(ClassNode formal, ClassNode instantiated, String name, Constraints constraints, Constraint ifToplevel) {
        if (name.equals(formal.getUnresolvedName())) constraints.addConstraint(instantiated, ifToplevel);
        if (!formal.redirect().equals(instantiated.redirect())) return;
        GenericsType[] fTypeArgs = formal.getGenericsTypes();
        GenericsType[] iTypeArgs = instantiated.getGenericsTypes();
        if(fTypeArgs == null || iTypeArgs == null || fTypeArgs.length != iTypeArgs.length) return;
        for (int i = 0; i < fTypeArgs.length; i++) {
            GenericsType fTypearg = fTypeArgs[i];
            GenericsType iTypearg = iTypeArgs[i];
            ClassNode fType = fTypearg.getType();
            ClassNode iType = iTypearg.getType();
            if (isSuper(fTypearg)) {
                if (iTypearg.isWildcard()) continue;
                fType = iType.isGenericsPlaceHolder() ? fType :
                        mapTypeFromSuper(iType.redirect(), iType.redirect(), fType);
                if (fType == null) continue;
                match(fType, iType, name, constraints, SUBTYPE);
            } else if (isExtends(fTypearg)) {
                if (iTypearg.isWildcard()) continue;
                iType = fType.isGenericsPlaceHolder() ? iType :
                        mapTypeFromSuper(fType.redirect(), fType.redirect(), iType);
                if (iType == null) continue;
                match(fType, iType, name, constraints, SUPERTYPE);
            } else {
                if (iTypearg.isWildcard()) continue;
                match(fType, iType, name, constraints, EQ);
            }
        }
    }
}
