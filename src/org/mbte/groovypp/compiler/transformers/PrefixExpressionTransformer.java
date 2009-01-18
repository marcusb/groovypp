package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;

public class PrefixExpressionTransformer extends ExprTransformer<PrefixExpression>{
    public Expression transform(PrefixExpression exp, CompilerTransformer compiler) {
        final Expression operand = exp.getExpression();
        if (operand instanceof VariableExpression) {
            return transformVariablePrefixExpression(exp, operand, compiler);
        }

        if (operand instanceof BinaryExpression && ((BinaryExpression)operand).getOperation().getType() == Types.LEFT_SQUARE_BRACKET) {
            return transformArrayPrefixExpression(exp, compiler);
        }

        if (operand instanceof PropertyExpression) {
            return transformPrefixPropertyExpression(exp, (PropertyExpression)operand, compiler);
        }

        compiler.addError("Prefix/Postfix operations allowed only with variable or property or array expressions", exp);
        return null;
    }

    private Expression transformPrefixPropertyExpression(final PrefixExpression exp, PropertyExpression pe, CompilerTransformer compiler) {

        Object property = pe.getProperty();
        String propName = null;
        if (!(property instanceof ConstantExpression) || !(((ConstantExpression) property).getValue() instanceof String)) {
          compiler.addError("Non-static property name", pe);
          return null;
        }
        else {
          propName = (String) ((ConstantExpression) property).getValue();
        }

        final BytecodeExpr object;
        final ClassNode type;
        if (pe.getObjectExpression() instanceof ClassExpression) {
            object = null;
            type = pe.getObjectExpression().getType();
        }
        else {
            object = (BytecodeExpr) compiler.transform(pe.getObjectExpression());
            type = object.getType();
        }

        final FieldNode propertyNode = compiler.findField (type, propName);
        if (propertyNode == null) {
            compiler.addError("Can't find property '" + propName + "' for type " + type.getName(), pe);
            return null;
        }

        if (object == null && !propertyNode.isStatic()) {
            compiler.addError("Can't access non-static property '" + propName + "' for type " + type.getName(), pe);
            return null;
        }

        if (!TypeUtil.isNumericalType(propertyNode.getType())) {
            compiler.addError("Prefix/Postfix operations applicable only to numerical types", exp);
            return null;
        }

        return new BytecodeExpr(exp, propertyNode.getType()) {
            protected void compile() {
                final ClassNode type = propertyNode.getType();
                final ClassNode primType = ClassHelper.getUnwrapper(type);
                int op = GETFIELD;
                if (propertyNode.isStatic()) {
                    op = GETSTATIC;
                }

                if (object != null) {
                    object.visit(mv);
                    if (op == GETSTATIC) {
                        if (ClassHelper.long_TYPE == object.getType() || ClassHelper.double_TYPE == object.getType())
                            mv.visitInsn(POP2);
                        else
                            mv.visitInsn(POP);
                    }
                    else {
                        mv.visitInsn(DUP);
                    }
                }

                // ?obj, ?obj
                mv.visitFieldInsn(op, BytecodeHelper.getClassInternalName(propertyNode.getDeclaringClass()), propertyNode.getName(), BytecodeHelper.getTypeDescription(propertyNode.getType()));

                // value, ?obj
                if (op == GETSTATIC) {
                    // value
                }
                else {
                    // value obj
                }

                // value ?obj
                if (type != primType)
                   unbox(primType);
                incOrDecPrimitive(primType, exp.getOperation().getType());
                if (type != primType) {
                   box(primType);
                   mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(type));
                }
                // newvalue ?obj

                op = PUTFIELD;
                if (propertyNode.isStatic()) {
                    op = PUTSTATIC;
                    dup(type);
                }
                else {
                    if (ClassHelper.long_TYPE == type || ClassHelper.double_TYPE == type)
                        mv.visitInsn(DUP2_X1);
                    else
                        mv.visitInsn(DUP_X1);
                }

                mv.visitFieldInsn(op, BytecodeHelper.getClassInternalName(propertyNode.getDeclaringClass()), propertyNode.getName(), BytecodeHelper.getTypeDescription(propertyNode.getType()));
            }
        };
    }

    private Expression transformArrayPrefixExpression(final PrefixExpression exp, CompilerTransformer compiler) {
        BinaryExpression bin = (BinaryExpression) exp.getExpression();
        final BytecodeExpr arrExp = (BytecodeExpr) compiler.transform(bin.getLeftExpression());

        if (!arrExp.getType().isArray()) {
            compiler.addError("Array subscript operator applicable only to array types", exp);
            return null;
        }

        final BytecodeExpr indexExp = (BytecodeExpr) compiler.transform(bin.getRightExpression ());
        if (!TypeUtil.isNumericalType(indexExp.getType())) {
            compiler.addError("Array subscript index should be integer", exp);
            return null;
        }

        final ClassNode type = arrExp.getType().getComponentType();
        return new BytecodeExpr(exp, type) {
            protected void compile() {
                final ClassNode primType = ClassHelper.getUnwrapper(type);

                arrExp.visit(mv);
                indexExp.visit(mv);
                toInt (indexExp.getType());
                mv.visitInsn(DUP2);
                loadArray(type);

                if (type != primType)
                   unbox(primType);
                // val, idx, arr, val
                incOrDecPrimitive(primType, exp.getOperation().getType());

                if (type != primType)
                   box(primType);

                // val, idx, arr
                if (type == ClassHelper.double_TYPE || type == ClassHelper.long_TYPE)
                    mv.visitInsn(DUP2_X2);
                else
                    mv.visitInsn(DUP_X2);

                storeArray(type);
            }
        };
    }

    private Expression transformVariablePrefixExpression(final PrefixExpression exp, Expression operand, CompilerTransformer compiler) {
        VariableExpression ve = (VariableExpression) operand;
        if (ve.isThisExpression() || ve.isSuperExpression()) {
            compiler.addError("Prefix/Postfix operations can not be applied to 'this' and 'super'", exp);
            return null;
        }

        final org.codehaus.groovy.classgen.Variable var = compiler.compileStack.getVariable(ve.getName(), true);
        if (!TypeUtil.isNumericalType(var.getType())) {
            compiler.addError("Prefix/Postfix operations applicable only to numerical types", exp);
            return null;
        }

        return new BytecodeExpr(exp, exp.getType()) {
            protected void compile() {
                final ClassNode type = var.getType();
                final ClassNode primType = ClassHelper.getUnwrapper(type);
                load(type, var.getIndex());
                if (type != primType)
                   unbox(primType);
                incOrDecPrimitive(primType, exp.getOperation().getType());
                if (type != primType)
                   box(primType);
                dup(type);
                store (var);
            }
        };
    }
}
