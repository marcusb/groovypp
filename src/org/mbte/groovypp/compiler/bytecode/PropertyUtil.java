package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.classgen.Verifier;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import groovy.lang.CompilePolicy;

public class PropertyUtil {
    public static BytecodeExpr createGetProperty(PropertyExpression exp, CompilerTransformer compiler, String propName, final BytecodeExpr object, Object prop, boolean needsObjectIfStatic) {
        if (prop instanceof MethodNode)
            return new ResolvedGetterBytecodeExpr(exp, (MethodNode) prop, object, needsObjectIfStatic);

        if (prop instanceof PropertyNode)
            return new ResolvedPropertyBytecodeExpr(exp, (PropertyNode) prop, object, null, needsObjectIfStatic);

        if (prop instanceof FieldNode)
            return new ResolvedFieldBytecodeExpr(exp, (FieldNode) prop, object, null, needsObjectIfStatic);

        if (object.getType().isArray() && "length".equals(propName)) {
            return new BytecodeExpr(exp, ClassHelper.int_TYPE) {
                protected void compile() {
                    object.visit(mv);
                    mv.visitInsn(ARRAYLENGTH);
                }
            };
        }

        return dynamicOrFail(exp, compiler, propName, object, null);
    }

    public static BytecodeExpr createSetProperty(ASTNode parent, CompilerTransformer compiler, String propName, BytecodeExpr object, BytecodeExpr value, Object prop, boolean needsObjectIfStatic) {
        if (prop instanceof MethodNode)
            return new ResolvedMethodBytecodeExpr(parent, (MethodNode) prop, object, new ArgumentListExpression(value));

        if (prop instanceof PropertyNode)
            return new ResolvedPropertyBytecodeExpr(parent, (PropertyNode) prop, object, value, needsObjectIfStatic);

        if (prop instanceof FieldNode)
            return new ResolvedFieldBytecodeExpr(parent, (FieldNode) prop, object, value, needsObjectIfStatic);

        return dynamicOrFail(parent, compiler, propName, object, value);
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

        final FieldNode field = compiler.findField(type, name);
        if (field != null)
            return field;

        final String setterName = "set" + Verifier.capitalize(name);
        mn = compiler.findMethod(type, setterName, new ClassNode[] {TypeUtil.NULL_TYPE});
        if (mn != null) {
            final PropertyNode res = new PropertyNode(name, mn.getModifiers(), mn.getParameters()[0].getType(), mn.getDeclaringClass(), null, null, null);
            res.setDeclaringClass(mn.getDeclaringClass());
            return res;
        }

        return null;
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

    private static BytecodeExpr dynamicOrFail(ASTNode exp, CompilerTransformer compiler, String propName, BytecodeExpr object, BytecodeExpr value) {
        if (compiler.policy == CompilePolicy.STATIC) {
            compiler.addError("Can't resolve property "+ propName, exp);
            return null;
        }
        else
            return createDynamicCall(exp, propName, object, value);
    }

    private static BytecodeExpr createDynamicCall(ASTNode exp, final String propName, final BytecodeExpr object, final BytecodeExpr value) {
        return new UnresolvedLeftExpr(exp, value, object, propName);
    }
}
