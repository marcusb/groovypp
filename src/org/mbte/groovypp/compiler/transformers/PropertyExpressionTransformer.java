package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.Verifier;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedMethodBytecodeExpr;

public class PropertyExpressionTransformer extends ExprTransformer<PropertyExpression>{
    public Expression transform(PropertyExpression expression, CompilerTransformer compiler) {
        if (expression.isSpreadSafe()) {
            compiler.addError("Spread operator is not supported yet by static compiler", expression);
            return null;
        }

        Object property = expression.getProperty();
        String propName = null;
        if (!(property instanceof ConstantExpression) || !(((ConstantExpression) property).getValue() instanceof String)) {
          compiler.addError("Non-static property name", expression);
          return null;
        }
        else {
          propName = (String) ((ConstantExpression) property).getValue();
        }

        final BytecodeExpr object;
        final ClassNode type;
        if (expression.getObjectExpression() instanceof ClassExpression) {
            object = null;
            type = expression.getObjectExpression().getType();
        }
        else {
            object = (BytecodeExpr) compiler.transform(expression.getObjectExpression());
            type = object.getType();
        }

        final String getterName = "get" + Verifier.capitalize(propName);
        MethodNode mn = compiler.findMethod(type, getterName, ClassNode.EMPTY_ARRAY);
        if (mn != null)
            return new ResolvedMethodBytecodeExpr(expression, mn, object, new ArgumentListExpression());

        final PropertyNode pnode = object != null ? object.getType().getProperty(expression.getPropertyAsString()) : null;
        if (pnode != null) {
            return new BytecodeExpr(expression, pnode.getType()) {
                protected void compile() {
                    object.visit(mv);
                    mv.visitMethodInsn(INVOKEVIRTUAL, BytecodeHelper.getClassInternalName(object.getType()), getterName, "()" + BytecodeHelper.getTypeDescription(pnode.getType()));
                }
            };
        }

        final FieldNode propertyNode = compiler.findField (type, propName);
        if (propertyNode == null) {
            compiler.addError("Can't find property '" + propName + "' for type " + type.getName(), expression);
            return null;
        }

        if (object == null && !propertyNode.isStatic()) {
            compiler.addError("Can't access non-static property '" + propName + "' for type " + type.getName(), expression);
            return null;
        }

        final boolean safe = expression.isSafe();
        final BytecodeExpr result = new MyBytecodeExpr(expression, propertyNode, object);

        result.setSourcePosition(expression);
        return result;
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