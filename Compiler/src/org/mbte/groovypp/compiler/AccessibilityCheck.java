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

package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.ClassNode;
import org.objectweb.asm.Opcodes;

public class AccessibilityCheck {
    public static boolean isAccessible(int modifiers,
                                       ClassNode declaringClass,
                                       ClassNode placeClass,
                                       ClassNode accessType) {
        if (accessType != null && !isAccessible(accessType.getModifiers(), accessType.getOuterClass(), placeClass, null)) return false;
        if (declaringClass == null) return true;
        if ((modifiers & Opcodes.ACC_PRIVATE) != 0) {
            return getToplevelClass(declaringClass).equals(getToplevelClass(placeClass));
        } else if ((modifiers & Opcodes.ACC_PROTECTED) != 0) {
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
