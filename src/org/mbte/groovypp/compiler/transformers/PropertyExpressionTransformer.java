package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.Verifier;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.ClosureMethodNode;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedMethodBytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedPropertyBytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedFieldBytecodeExpr;

public class PropertyExpressionTransformer extends ExprTransformer<PropertyExpression>{
    public Expression transform(PropertyExpression exp, CompilerTransformer compiler) {
        if (exp.isSpreadSafe()) {
            compiler.addError("Spread operator is not supported yet by static compiler", exp);
            return null;
        }

        Object property = exp.getProperty();
        String propName = null;
        if (!(property instanceof ConstantExpression) || !(((ConstantExpression) property).getValue() instanceof String)) {
          compiler.addError("Non-static property name", exp);
          return null;
        }
        else {
          propName = (String) ((ConstantExpression) property).getValue();
        }

        final BytecodeExpr object;
        final ClassNode type;

        if (exp.getObjectExpression() instanceof ClassExpression) {
            object = null;
            type = ClassHelper.getWrapper(exp.getObjectExpression().getType());

            Object prop = resolveGetProperty(type, propName, compiler);

            return createResultExpr(exp, compiler, propName, object, prop);
        }
        else {
            if (exp.getObjectExpression().equals(VariableExpression.THIS_EXPRESSION) && compiler.methodNode instanceof ClosureMethodNode) {
                int level = 0;
                for( ClosureMethodNode cmn = (ClosureMethodNode) compiler.methodNode; cmn != null; cmn = cmn.getOwner(), level++ ) {
                    ClassNode thisType = cmn.getParameters()[0].getType();

                    Object prop = resolveGetProperty(thisType, propName, compiler);
                    if (prop != null) {
                        final int level1 = level;
                        object = new BytecodeExpr(exp.getObjectExpression(), thisType) {
                            protected void compile() {
                                mv.visitVarInsn(ALOAD, 0);
                                for (int i = 0; i != level1; ++i) {
                                    mv.visitTypeInsn(CHECKCAST, "groovy/lang/Closure");
                                    mv.visitMethodInsn(INVOKEVIRTUAL, "groovy/lang/Closure", "getOwner", "()Ljava/lang/Object;");
                                }
                                mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(getType()));
                            }
                        };

                        return createResultExpr(exp, compiler, propName, object, prop);
                    }

                    // checkDelegate
                    if (thisType.implementsInterface(TypeUtil.TCLOSURE)) {
                        final ClassNode tclosure = thisType.getInterfaces()[0];
                        final GenericsType[] genericsTypes = tclosure.getGenericsTypes();
                        if (genericsTypes != null) {
                            final ClassNode delegateType = genericsTypes[0].getType();
                            prop = resolveGetProperty(delegateType, propName, compiler);
                            if (prop != null) {
                                final int level3 = level;
                                object = new BytecodeExpr(exp.getObjectExpression(), delegateType) {
                                    protected void compile() {
                                        mv.visitVarInsn(ALOAD, 0);
                                        for (int i = 0; i != level3; ++i) {
                                            mv.visitTypeInsn(CHECKCAST, "groovy/lang/Closure");
                                            mv.visitMethodInsn(INVOKEVIRTUAL, "groovy/lang/Closure", "getOwner", "()Ljava/lang/Object;");
                                        }
                                        mv.visitTypeInsn(CHECKCAST, "groovy/lang/Closure");
                                        mv.visitMethodInsn(INVOKEVIRTUAL, "groovy/lang/Closure", "getDelegate", "()Ljava/lang/Object;");
                                        mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(getType()));
                                    }
                                };
                                return createResultExpr(exp, compiler, propName, object, prop);
                            }
                        }
                    }
                }

                Object prop = resolveGetProperty(compiler.classNode, propName, compiler);
                if (prop != null) {
                    final int level2 = level;
                    object = new BytecodeExpr(exp.getObjectExpression(), compiler.classNode) {
                        protected void compile() {
                            mv.visitVarInsn(ALOAD, 0);
                            for (int i = 0; i != level2; ++i) {
                                mv.visitTypeInsn(CHECKCAST, "groovy/lang/Closure");
                                mv.visitMethodInsn(INVOKEVIRTUAL, "groovy/lang/Closure", "getOwner", "()Ljava/lang/Object;");
                            }
                            mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(getType()));
                        }
                    };
                    return createResultExpr(exp, compiler, propName, object, prop);
                }

                compiler.addError("Can't resolve property " + propName, exp);
                return null;
            } else {
                object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
                type = ClassHelper.getWrapper(object.getType());

                Object prop = resolveGetProperty(type, propName, compiler);
                return createResultExpr(exp, compiler, propName, object, prop);
            }
        }
    }

    private Expression createResultExpr(PropertyExpression exp, CompilerTransformer compiler, String propName, BytecodeExpr object, Object prop) {
        if (prop instanceof MethodNode)
            return new ResolvedMethodBytecodeExpr(exp, (MethodNode) prop, object, new ArgumentListExpression());

        if (prop instanceof PropertyNode)
            return new ResolvedPropertyBytecodeExpr(exp, (PropertyNode) prop, object, null);

        if (prop instanceof FieldNode)
            return new ResolvedFieldBytecodeExpr(exp, (FieldNode) prop, object, null);

        compiler.addError("Can't resolve property " + propName, exp);
        return null;
    }

    public Object resolveGetProperty (ClassNode type, String name, CompilerTransformer compiler) {
        final String getterName = "get" + Verifier.capitalize(name);
        MethodNode mn = compiler.findMethod(type, getterName, ClassNode.EMPTY_ARRAY);
        if (mn != null)
            return mn;

        final PropertyNode pnode = type.getProperty(name);
        if (pnode != null) {
            return pnode;
        }

        return compiler.findField (type, name);
    }

    public Object resolveSetProperty (ClassNode type, String name, CompilerTransformer compiler) {
        final String getterName = "set" + Verifier.capitalize(name);
        MethodNode mn = compiler.findMethod(type, getterName, ClassNode.EMPTY_ARRAY);
        if (mn != null)
            return mn;

        final PropertyNode pnode = type.getProperty(name);
        if (pnode != null) {
            return pnode;
        }

        return compiler.findField (type, name);
    }

    private static class MyBytecodeExpr extends BytecodeExpr {
        private final FieldNode propertyNode;
        private final BytecodeExpr object;

        public MyBytecodeExpr(PropertyExpression expression, FieldNode propertyNode, BytecodeExpr object) {
            super(expression, propertyNode.getType());
            this.propertyNode = propertyNode;
            this.object = object;
        }

        protected void compile() {
            int op = GETFIELD;
            if (propertyNode.isStatic()) {
                op = GETSTATIC;
            }
            if (object != null)
                object.visit(mv);

            if (op == GETSTATIC && object != null) {
                if (ClassHelper.long_TYPE == object.getType() || ClassHelper.double_TYPE == object.getType())
                    mv.visitInsn(POP2);
                else
                    mv.visitInsn(POP);
            }

            mv.visitFieldInsn(op, BytecodeHelper.getClassInternalName(propertyNode.getDeclaringClass()), propertyNode.getName(), BytecodeHelper.getTypeDescription(propertyNode.getType()));
        }
    }
}
