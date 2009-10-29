package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GenericsType;
import static org.mbte.groovypp.compiler.TypeUnification.Constraint.*;
import static org.mbte.groovypp.compiler.TypeUtil.*;

/**
 * @author ven
 */
public class TypeUnification {
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
            NextParam:
            for (int j = 0; j < Math.min(formals.length, instantiateds.length); ++j) {
                ClassNode formal = formals[j];
                ClassNode instantiated = instantiateds[j];
                while (formal.isArray()) {
                    if (!instantiated.isArray()) continue NextParam;
                    formal = formal.getComponentType();
                    instantiated = instantiated.getComponentType();
                }
                if (instantiated.isArray()) continue;
                formal = TypeUtil.wrapSafely(formal);
                instantiated = TypeUtil.wrapSafely(instantiated);

                // this is just one variance, be sure to add another if ever needed.
                instantiated = formal.isGenericsPlaceHolder() ? instantiated :
                        mapTypeFromSuper(formal.redirect(), formal.redirect(), instantiated);
                if (instantiated == null) continue;

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
            while (fType.isArray()) {
                if (!iType.isArray()) continue;
                fType = fType.getComponentType();
                iType = iType.getComponentType();
            }
            if (iType.isArray()) continue;
            fType = TypeUtil.wrapSafely(fType);
            iType = TypeUtil.wrapSafely(iType);

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
