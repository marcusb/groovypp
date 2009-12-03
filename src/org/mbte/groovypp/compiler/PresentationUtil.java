package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.Parameter;
import org.objectweb.asm.Opcodes;

/**
 * @author ven
 */
public class PresentationUtil {
    public static String getText(ClassNode type) {
        StringBuilder builder = new StringBuilder();
        if (type instanceof ClosureClassNode) {
            builder.append("{ ");
            Parameter[] parameters = ((ClosureClassNode) type).getDoCallMethod().getParameters();
            for (int i = 0; i < parameters.length; i++) {
                if (i > 0) builder.append(", ");
                getText(parameters[i].getType(), builder);
            }
            builder.append(" -> ...}");
        } else {
            if ((type.getModifiers() & Opcodes.ACC_SYNTHETIC) != 0) {
                // This is synthetic closure node, present its super type.
                if (type.getInterfaces().length > 0) {
                    type = type.getInterfaces()[0];
                } else if (type.getSuperClass() != null) {
                    type = type.getSuperClass();
                }
            }
            getText(type, builder);
        }
        return builder.toString();
    }

    private static void getText(ClassNode type, StringBuilder builder) {
        builder.append(type.getNameWithoutPackage());
        GenericsType[] generics = type.getGenericsTypes();
        if (generics != null && generics.length > 0) {
            builder.append("<");
            for (int i = 0; i < generics.length; i++) {
                if (i > 0) builder.append(",");
                if (generics[i].isWildcard()) {
                    if (TypeUtil.isExtends(generics[i])) {
                        builder.append("? extends ");
                    } else if (TypeUtil.isSuper(generics[i])) {
                        builder.append("? super ");
                    } else {
                        builder.append("?");
                        continue;
                    }
                }
                getText(generics[i].getType(), builder);
            }
            builder.append(">");
        }
    }
}
