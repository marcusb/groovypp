package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.ClassNode;
import org.objectweb.asm.Opcodes;

/**
 * @author ven
 */
public class AccessibilityCheck {
    public static boolean isAccessible(int modifiers,
                                       ClassNode declaringClass,
                                       ClassNode placeClass,
                                       ClassNode accessType) {
        if (accessType != null && !isAccessible(accessType.getModifiers(), accessType.getOuterClass(), placeClass, null)) return false;
        if ((modifiers & Opcodes.ACC_PRIVATE) != 0) {
            if (declaringClass == null) return true;
            return getToplevelClass(declaringClass).equals(getToplevelClass(placeClass));
        } else if ((modifiers & Opcodes.ACC_PROTECTED) != 0) {
            if (declaringClass == null) return true;
            if (samePackage(declaringClass, placeClass)) return true;
            while (placeClass != null) {
                if (placeClass.isDerivedFrom(declaringClass)) {
                    return accessType == null || accessType.isDerivedFrom(placeClass);
                }
                if ((placeClass.getModifiers() & Opcodes.ACC_STATIC) != 0) break;
                placeClass = placeClass.getOuterClass();
            }
            return false;
        } else if ((modifiers & Opcodes.ACC_PUBLIC) != 0) {
            return true;
        }
        // package local
        if (!samePackage(declaringClass, placeClass)) return false;
        // check the entire inheritance chain.
        ClassNode clazz = null;
        if (accessType != null) {
            clazz = accessType;
        } else {
            while (placeClass != null) {
                if (placeClass.isDerivedFrom(declaringClass)) {
                    clazz = placeClass;
                    break;
                }
                if ((placeClass.getModifiers() & Opcodes.ACC_STATIC) != 0) break;
                placeClass = placeClass.getOuterClass();
            }
        }
        while (clazz != null && !clazz.equals(declaringClass)) {
            if (!samePackage(declaringClass, clazz)) return false;
            clazz = clazz.getSuperClass();
        }
        return true;
    }

    private static ClassNode getToplevelClass(ClassNode node) {
        ClassNode toplevel = node;
        while (toplevel.getOuterClass() != null) toplevel = toplevel.getOuterClass();
        return toplevel;
    }

    private static boolean samePackage(ClassNode c1, ClassNode c2) {
        if (c1.redirect().equals(c2.redirect())) return true; 
        String name1 = c1.getPackageName();
        String name2 = c2.getPackageName();
        if (name1 == null) name1 = "";  // why is it nullable?
        if (name2 == null) name2 = "";
        return name1.equals(name2);
    }
}
