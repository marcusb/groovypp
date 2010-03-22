package org.mbte.groovypp.compiler.bytecode;

import groovy.lang.TypePolicy;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.Verifier;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.PresentationUtil;
import org.mbte.groovypp.compiler.TypeUtil;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class PropertyUtil {
    public static final Object GET_MAP = new Object ();

    public static BytecodeExpr createGetProperty(final PropertyExpression exp, final CompilerTransformer compiler, String propName, final BytecodeExpr object, Object prop) {
        if (prop instanceof MethodNode) {
            MethodNode method = (MethodNode) prop;
            if ((method.getModifiers() & Opcodes.ACC_PRIVATE) != 0 && method.getDeclaringClass() != compiler.classNode) {
                MethodNode delegate = compiler.context.getMethodDelegate(method);
                new ResolvedGetterBytecodeExpr(exp, delegate, object, compiler, propName);
            }
            return new ResolvedGetterBytecodeExpr(exp, method, object, compiler, propName);
        }

        if (prop instanceof PropertyNode) {
            return new ResolvedPropertyBytecodeExpr(exp, (PropertyNode) prop, object, null, compiler);
        }

        if (prop instanceof FieldNode) {
            FieldNode field = (FieldNode) prop;
            if ((field.getModifiers() & Opcodes.ACC_PRIVATE) != 0 && field.getDeclaringClass() != compiler.classNode) {
                MethodNode getter = compiler.context.getFieldGetter(field);
                return new ResolvedGetterBytecodeExpr.Accessor(field, exp, getter, object, compiler);
            }
            return new ResolvedFieldBytecodeExpr(exp, field, object, null, compiler);
        }

        if (object == null && "this".equals(propName)) {
            ClassNode curr = compiler.classNode;
            while (curr != null) {
                final FieldNode field = curr.getDeclaredField("this$0");
                if (field == null)
                    break;

                compiler.context.setOuterClassInstanceUsed(curr);
                curr = field.getType();
                if (curr.equals(exp.getObjectExpression().getType())) {
                    return new BytecodeExpr(exp, curr){
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

        if (object != null && object.getType().isArray() && "length".equals(propName)) {
            return new BytecodeExpr(exp, ClassHelper.int_TYPE) {
                protected void compile(MethodVisitor mv) {
                    object.visit(mv);
                    mv.visitInsn(ARRAYLENGTH);
                }
            };
        }

        if (prop == GET_MAP) {
            return new ResolvedLeftMapExpr(exp, object, propName);
        }

        return dynamicOrFail(exp.getProperty(), compiler, propName, object, null);
    }

    public static BytecodeExpr createSetProperty(ASTNode parent, CompilerTransformer compiler, String propName, BytecodeExpr object, BytecodeExpr value, Object prop) {
        if (prop instanceof MethodNode) {
            return new ResolvedMethodBytecodeExpr.Setter(parent, (MethodNode) prop, object, new ArgumentListExpression(value), compiler);
        }

        if (prop instanceof PropertyNode) {
            final PropertyNode propertyNode = (PropertyNode) prop;
            if ((propertyNode.getModifiers() & Opcodes.ACC_FINAL) != 0) {
                final FieldNode fieldNode = compiler.findField(propertyNode.getDeclaringClass(), propName);
                return new ResolvedFieldBytecodeExpr(parent, fieldNode, object, value, compiler);
            }

            return new ResolvedPropertyBytecodeExpr(parent, propertyNode, object, value, compiler);
        }

        if (prop instanceof FieldNode) {
            final FieldNode field = (FieldNode) prop;
            if ((field.getModifiers() & Opcodes.ACC_PRIVATE) != 0 && field.getDeclaringClass() != compiler.classNode) {
                MethodNode setter = compiler.context.getFieldSetter(field);
                return new ResolvedMethodBytecodeExpr.Setter(parent, setter, object, new ArgumentListExpression(value), compiler);
            }
            return new ResolvedFieldBytecodeExpr(parent, field, object, value, compiler);
        }

        return dynamicOrFail(parent, compiler, propName, object, value);
    }

    public static EmptyStatement NO_CODE = new EmptyStatement();

    public static Object resolveGetProperty(ClassNode type, String name, CompilerTransformer compiler, boolean onlyStatic, boolean isSameObject) {
        final FieldNode field = compiler.findField(type, name);

        String getterName = "get" + Verifier.capitalize(name);
        MethodNode mn = compiler.findMethod(type, getterName, ClassNode.EMPTY_ARRAY, false);
        if (mn != null && !mn.isAbstract() && (!onlyStatic || mn.isStatic())) {
            if (mn == compiler.methodNode && isSameObject && field != null) return field;  // Access inside the getter itself is to the field.
            return mn;
        }

        if (mn == null) {
            getterName = "is" + Verifier.capitalize(name);
            mn = compiler.findMethod(type, getterName, ClassNode.EMPTY_ARRAY, false);
            if (mn != null && !mn.isAbstract() &&
                mn.getReturnType().equals(ClassHelper.boolean_TYPE) && (!onlyStatic || mn.isStatic())) {
                if (mn == compiler.methodNode && isSameObject && field != null) return field;  // Access inside the getter itself is to the field.
                return mn;
            }
        }

        final PropertyNode pnode = compiler.findProperty(type, name);
        if (pnode != null && (!onlyStatic || pnode.isStatic())) {
            return pnode;
        }

        if (mn != null && (!onlyStatic || mn.isStatic()))
            return mn;

        if (field != null && (!onlyStatic || field.isStatic()))
            return field;

        final String setterName = "set" + Verifier.capitalize(name);
        mn = compiler.findMethod(type, setterName, new ClassNode[]{TypeUtil.NULL_TYPE}, false);
        if (mn != null && (!onlyStatic || mn.isStatic()) && mn.getReturnType() == ClassHelper.VOID_TYPE) {
            final PropertyNode res = new PropertyNode(name, mn.getModifiers(), mn.getParameters()[0].getType(), mn.getDeclaringClass(), null, NO_CODE, null);
            res.setDeclaringClass(mn.getDeclaringClass());
            return res;
        }

        if (!onlyStatic && !isSameObject && type.implementsInterface(ClassHelper.MAP_TYPE)) {
            return GET_MAP;
        }
        
        return null;
    }

    public static Object resolveSetProperty(ClassNode type, String name, ClassNode arg, CompilerTransformer compiler, boolean isSameObject) {
        final FieldNode field = compiler.findField(type, name);
        final String setterName = "set" + Verifier.capitalize(name);
        MethodNode mn = compiler.findMethod(type, setterName, new ClassNode[]{arg}, false);
        if (mn != null && mn.getReturnType() == ClassHelper.VOID_TYPE) {
            if (mn == compiler.methodNode && isSameObject && field != null) return field;
            return mn;
        }

        final PropertyNode pnode = type.getProperty(name);
        if (pnode != null) {
            return pnode;
        }

        return field;
    }

    private static BytecodeExpr dynamicOrFail(ASTNode exp, CompilerTransformer compiler, String propName, BytecodeExpr object, BytecodeExpr value) {
        if (compiler.policy == TypePolicy.STATIC) {
            final ClassNode type = object != null ? object.getType() : compiler.classNode;
            compiler.addError("Cannot find property " + propName + " of class " + PresentationUtil.getText(type), exp);
            return null;
        } else
            return createDynamicCall(exp, propName, object, value);
    }

    private static BytecodeExpr createDynamicCall(ASTNode exp, final String propName, final BytecodeExpr object, final BytecodeExpr value) {
        return new UnresolvedLeftExpr(exp, value, object, propName);
    }

    public static boolean isStatic(Object prop) {
        if (prop instanceof MethodNode) return ((MethodNode) prop).isStatic();
        if (prop instanceof PropertyNode) return ((PropertyNode) prop).isStatic();
        if (prop instanceof FieldNode) return ((FieldNode) prop).isStatic();
        return false;
    }

    public static ClassNode getPropertyType(Object prop) {
        if (prop instanceof FieldNode) return ((FieldNode) prop).getType();
        if(prop instanceof PropertyNode) return ((PropertyNode) prop).getType();
        return ((MethodNode) prop).getReturnType();
    }
}
