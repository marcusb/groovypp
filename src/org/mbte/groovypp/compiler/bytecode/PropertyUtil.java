package org.mbte.groovypp.compiler.bytecode;

import groovy.lang.TypePolicy;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.classgen.Verifier;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.objectweb.asm.MethodVisitor;

public class PropertyUtil {
    public static BytecodeExpr createGetProperty(final PropertyExpression exp, final CompilerTransformer compiler, String propName, final BytecodeExpr object, Object prop, boolean needsObjectIfStatic) {
        if (prop instanceof MethodNode)
            return new ResolvedGetterBytecodeExpr(exp, (MethodNode) prop, object, needsObjectIfStatic, compiler);

        if (prop instanceof PropertyNode)
            return new ResolvedPropertyBytecodeExpr(exp, (PropertyNode) prop, object, null);

        if (prop instanceof FieldNode)
            return new ResolvedFieldBytecodeExpr(exp, (FieldNode) prop, object, null, compiler);

        if (object == null && "this".equals(propName)) {
            ClassNode cur = compiler.classNode;
            while (cur != null) {
                final FieldNode field = cur.getDeclaredField("this$0");
                if (field == null)
                    break;

                cur = field.getType();
                if (cur.equals(exp.getObjectExpression().getType())) {
                    return new BytecodeExpr(exp,cur){
                        protected void compile(MethodVisitor mv) {
                            ClassNode cur = compiler.classNode;
                            mv.visitVarInsn(ALOAD, 0);
                            while (!cur.equals(exp.getObjectExpression().getType())) {
                                final FieldNode field = cur.getDeclaredField("this$0");
                                mv.visitFieldInsn(GETFIELD, BytecodeHelper.getClassInternalName(cur), "this$0", BytecodeHelper.getTypeDescription(field.getType()));
                                cur = field.getType();
                            }
                        }
                    };
                }
            }
            return null;
        }

        if (object.getType().isArray() && "length".equals(propName)) {
            return new BytecodeExpr(exp, ClassHelper.int_TYPE) {
                protected void compile(MethodVisitor mv) {
                    object.visit(mv);
                    mv.visitInsn(ARRAYLENGTH);
                }
            };
        }

        return dynamicOrFail(exp, compiler, propName, object, null);
    }

    public static BytecodeExpr createSetProperty(ASTNode parent, CompilerTransformer compiler, String propName, BytecodeExpr object, BytecodeExpr value, Object prop) {
        if (prop instanceof MethodNode)
            return new ResolvedMethodBytecodeExpr(parent, (MethodNode) prop, object, new ArgumentListExpression(value), compiler);

        if (prop instanceof PropertyNode)
            return new ResolvedPropertyBytecodeExpr(parent, (PropertyNode) prop, object, value);

        if (prop instanceof FieldNode)
            return new ResolvedFieldBytecodeExpr(parent, (FieldNode) prop, object, value, compiler);

        return dynamicOrFail(parent, compiler, propName, object, value);
    }

    public static Object resolveGetProperty(ClassNode type, String name, CompilerTransformer compiler) {
        final FieldNode field = compiler.findField(type, name);

        String getterName = "get" + Verifier.capitalize(name);
        MethodNode mn = compiler.findMethod(type, getterName, ClassNode.EMPTY_ARRAY);
        if (mn != null && (field == null || !field.isPublic()))
            return mn;

        getterName = "is" + Verifier.capitalize(name);
        mn = compiler.findMethod(type, getterName, ClassNode.EMPTY_ARRAY);
        if (mn != null && mn.getReturnType().equals(ClassHelper.boolean_TYPE) && (field == null || !field.isPublic()))
            return mn;

        final PropertyNode pnode = compiler.findProperty(type, name);
        if (pnode != null) {
            return pnode;
        }

        if (field != null)
            return field;

        final String setterName = "set" + Verifier.capitalize(name);
        mn = compiler.findMethod(type, setterName, new ClassNode[]{TypeUtil.NULL_TYPE});
        if (mn != null) {
            final PropertyNode res = new PropertyNode(name, mn.getModifiers(), mn.getParameters()[0].getType(), mn.getDeclaringClass(), null, null, null);
            res.setDeclaringClass(mn.getDeclaringClass());
            return res;
        }

        return null;
    }

    public static Object resolveSetProperty(ClassNode type, String name, ClassNode arg, CompilerTransformer compiler) {
        final String getterName = "set" + Verifier.capitalize(name);
        MethodNode mn = compiler.findMethod(type, getterName, new ClassNode[]{arg});
        if (mn != null && mn.getReturnType() == ClassHelper.VOID_TYPE)
            return mn;

        final PropertyNode pnode = type.getProperty(name);
        if (pnode != null) {
            return pnode;
        }

        return compiler.findField(type, name);
    }

    private static BytecodeExpr dynamicOrFail(ASTNode exp, CompilerTransformer compiler, String propName, BytecodeExpr object, BytecodeExpr value) {
        if (compiler.policy == TypePolicy.STATIC) {
            compiler.addError("Can't find property " + propName + " of class " + object.getType().getName(), exp);
            return null;
        } else
            return createDynamicCall(exp, propName, object, value);
    }

    private static BytecodeExpr createDynamicCall(ASTNode exp, final String propName, final BytecodeExpr object, final BytecodeExpr value) {
        return new UnresolvedLeftExpr(exp, value, object, propName);
    }
}