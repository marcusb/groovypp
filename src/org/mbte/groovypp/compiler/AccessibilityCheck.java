package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.ClassNode;
import org.objectweb.asm.Opcodes;

/**
 * @author ven
 */
public class AccessibilityCheck {
    public static boolean isAccessible(int modifiers,
                                       ClassNode declaringClass,
                                       AnnotatedNode place,
                                       ClassNode accessType) {
        if (accessType != null && !isAccessible(accessType.getModifiers(), accessType.getDeclaringClass(), place, null)) return false;
        ClassNode placeClass = place instanceof  ClassNode ? (ClassNode)place : place.getDeclaringClass();
        if ((modifiers & Opcodes.ACC_PRIVATE) != 0) {
            if (declaringClass == null) return true;
            return getToplevelClass(declaringClass).equals(getToplevelClass(place));
        } else if ((modifiers & Opcodes.ACC_PROTECTED) != 0) {
            if (declaringClass == null) return true;
            if (samePackage(declaringClass, placeClass)) return true;
            return placeClass.isDerivedFrom(declaringClass);
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
                placeClass = placeClass.getDeclaringClass();
            }
        }
        while (clazz != null && !clazz.equals(declaringClass)) {
            if (!samePackage(declaringClass, clazz)) return false;
            clazz = clazz.getSuperClass();
        }
        return true;
    }

    private static ClassNode getToplevelClass(AnnotatedNode node) {
        ClassNode clazz = node instanceof ClassNode ? (ClassNode) node : node.getDeclaringClass();
        while (clazz.getDeclaringClass() != null) clazz = clazz.getDeclaringClass();
        return clazz;
    }

    private static boolean samePackage(ClassNode c1, ClassNode c2) {
        return c1.getPackageName().equals(c2.getPackageName());
    }
}
