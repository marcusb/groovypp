package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.classgen.Verifier;
import org.mbte.groovypp.compiler.CompilerTransformer;

public class PropertyUtil {
    public static BytecodeExpr createGetProperty(PropertyExpression exp, CompilerTransformer compiler, String propName, BytecodeExpr object, Object prop) {
        if (prop instanceof MethodNode)
            return new ResolvedGetterBytecodeExpr(exp, (MethodNode) prop, object);

        if (prop instanceof PropertyNode)
            return new ResolvedPropertyBytecodeExpr(exp, (PropertyNode) prop, object, null);

        if (prop instanceof FieldNode)
            return new ResolvedFieldBytecodeExpr(exp, (FieldNode) prop, object, null);

        compiler.addError("Can't resolve property " + propName, exp);
        return null;
    }

    public static BytecodeExpr createSetProperty(ASTNode parent, CompilerTransformer compiler, String propName, BytecodeExpr object, BytecodeExpr value, Object prop) {
        if (prop instanceof MethodNode)
            return new ResolvedMethodBytecodeExpr(parent, (MethodNode) prop, object, new ArgumentListExpression(value));

        if (prop instanceof PropertyNode)
            return new ResolvedPropertyBytecodeExpr(parent, (PropertyNode) prop, object, value);

        if (prop instanceof FieldNode)
            return new ResolvedFieldBytecodeExpr(parent, (FieldNode) prop, object, value);

        compiler.addError("Can't resolve property " + propName, parent);
        return null;
    }

    public static Object resolveGetProperty (ClassNode type, String name, CompilerTransformer compiler) {
        final String getterName = "get" + Verifier.capitalize(name);
        MethodNode mn = compiler.findMethod(type, getterName, ClassNode.EMPTY_ARRAY);
        if (mn != null)
            return mn;

        final PropertyNode pnode = compiler.findProperty(type, name);
        if (pnode != null) {
            return pnode;
        }

        return compiler.findField (type, name);
    }

    public static Object resolveSetProperty (ClassNode type, String name, ClassNode arg, CompilerTransformer compiler) {
        final String getterName = "set" + Verifier.capitalize(name);
        MethodNode mn = compiler.findMethod(type, getterName, new ClassNode[] {arg});
        if (mn != null)
            return mn;

        final PropertyNode pnode = type.getProperty(name);
        if (pnode != null) {
            return pnode;
        }

        return compiler.findField (type, name);
    }
}
