package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

public class PostfixExpressionTransformer extends ExprTransformer<PostfixExpression> {
    public Expression transform(final PostfixExpression exp, CompilerTransformer compiler) {
        final Expression operand = exp.getExpression();
        if (operand instanceof VariableExpression) {
            return transformPostfixVariableExpression(exp, operand, compiler);
        }

        if (operand instanceof BinaryExpression && ((BinaryExpression) operand).getOperation().getType() == Types.LEFT_SQUARE_BRACKET) {
            return transformArrayPostfixExpression(exp, compiler);
        }

        if (operand instanceof PropertyExpression) {
            return transformPostfixPropertyExpression(exp, (PropertyExpression) operand, compiler);
        }

        final BytecodeExpr oper = (BytecodeExpr) compiler.transform(operand);
        ClassNode vtype = oper.getType();
        if (TypeUtil.isNumericalType(vtype)) {
            return new BytecodeExpr(exp, vtype) {
                protected void compile(MethodVisitor mv) {
                    oper.visit(mv);
                }
            };
        }

        if (ClassHelper.isPrimitiveType(vtype))
            vtype = ClassHelper.getWrapper(vtype);

        final MethodNode methodNode = compiler.findMethod(vtype, "next", ClassNode.EMPTY_ARRAY);
        if (methodNode == null) {
            compiler.addError("Can't find method next() for type " + vtype.getName(), exp);
            return null;
        }

//        if (!TypeUtil.isDirectlyAssignableFrom(vtype, methodNode.getReturnType())) {
//            compiler.addError("Can't find method next() for type " + vtype.getName(), exp);
//            return null;
//        }

        final BytecodeExpr nextCall = (BytecodeExpr) compiler.transform(new MethodCallExpression(
                new BytecodeExpr(exp, vtype) {
                    protected void compile(MethodVisitor mv) {
                    }
                },
                "next",
                new ArgumentListExpression()
        ));

        return new BytecodeExpr(exp, vtype) {
            protected void compile(MethodVisitor mv) {
                oper.visit(mv);
                mv.visitInsn(DUP);
                nextCall.visit(mv);
                pop(nextCall.getType(), mv);
            }
        };
    }

    private Expression transformPostfixPropertyExpression(final PostfixExpression exp, PropertyExpression pe, CompilerTransformer compiler) {

        Object property = pe.getProperty();
        String propName = null;
        if (!(property instanceof ConstantExpression) || !(((ConstantExpression) property).getValue() instanceof String)) {
            compiler.addError("Non-static property name", pe);
            return null;
        } else {
            propName = (String) ((ConstantExpression) property).getValue();
        }

        final BytecodeExpr object;
        final ClassNode type;
        if (pe.getObjectExpression() instanceof ClassExpression) {
            object = null;
            type = pe.getObjectExpression().getType();
        } else {
            object = (BytecodeExpr) compiler.transform(pe.getObjectExpression());
            type = object.getType();
        }

        final FieldNode propertyNode = compiler.findField(type, propName);
        if (propertyNode == null) {
            compiler.addError("Can't find property '" + propName + "' for type " + type.getName(), pe);
            return null;
        }

        if (object == null && !propertyNode.isStatic()) {
            compiler.addError("Can't access non-static property '" + propName + "' for type " + type.getName(), pe);
            return null;
        }

        if (TypeUtil.isNumericalType(propertyNode.getType())) {
            return new BytecodeExpr(exp, propertyNode.getType()) {
                protected void compile(MethodVisitor mv) {
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
                        } else {
                            mv.visitInsn(DUP);
                        }
                    }

                    // ?obj, ?obj
                    mv.visitFieldInsn(op, BytecodeHelper.getClassInternalName(propertyNode.getDeclaringClass()), propertyNode.getName(), BytecodeHelper.getTypeDescription(propertyNode.getType()));

                    // value, ?obj
                    if (op == GETSTATIC) {
                        // value
                        dup(type, mv);
                        // value value
                    } else {
                        // value obj
                        if (ClassHelper.long_TYPE == type || ClassHelper.double_TYPE == type)
                            mv.visitInsn(DUP2_X1);
                        else
                            mv.visitInsn(DUP_X1);
                        // value obj value
                    }

                    // value ?obj value
                    if (type != primType)
                        unbox(primType, mv);
                    incOrDecPrimitive(primType, exp.getOperation().getType(), mv);
                    if (type != primType) {
                        box(primType, mv);
                        mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(type));
                    }
                    // newvalue ?obj value

                    op = PUTFIELD;
                    if (propertyNode.isStatic()) {
                        op = PUTSTATIC;
                    }

                    mv.visitFieldInsn(op, BytecodeHelper.getClassInternalName(propertyNode.getDeclaringClass()), propertyNode.getName(), BytecodeHelper.getTypeDescription(propertyNode.getType()));
                }
            };
        } else {
            return new BytecodeExpr(exp, propertyNode.getType()) {
                protected void compile(MethodVisitor mv) {
                    if (object != null)
                        object.visit(mv);
                }
            };
        }
    }

    private Expression transformArrayPostfixExpression(final PostfixExpression exp, CompilerTransformer compiler) {
        BinaryExpression bin = (BinaryExpression) exp.getExpression();
        final BytecodeExpr arrExp = (BytecodeExpr) compiler.transform(bin.getLeftExpression());

        if (!arrExp.getType().isArray()) {
            compiler.addError("Array subscript operator applicable only to array types", exp);
            return null;
        }

        final BytecodeExpr indexExp = (BytecodeExpr) compiler.transform(bin.getRightExpression());
        if (!TypeUtil.isNumericalType(indexExp.getType())) {
            compiler.addError("Array subscript index should be integer", exp);
            return null;
        }

        final ClassNode type = arrExp.getType().getComponentType();
        return new BytecodeExpr(exp, type) {
            protected void compile(MethodVisitor mv) {
                final ClassNode primType = ClassHelper.getUnwrapper(type);

                arrExp.visit(mv);
                indexExp.visit(mv);
                toInt(indexExp.getType(), mv);
                mv.visitInsn(DUP2);
                loadArray(type, mv);

                // val, idx, arr
                if (type == ClassHelper.double_TYPE || type == ClassHelper.long_TYPE)
                    mv.visitInsn(DUP2_X2);
                else
                    mv.visitInsn(DUP_X2);

                if (type != primType)
                    unbox(primType, mv);
                // val, idx, arr, val
                incOrDecPrimitive(primType, exp.getOperation().getType(), mv);

                if (type != primType)
                    box(primType, mv);

                // newVal, idx, arr, val
                storeArray(type, mv);
            }
        };
    }

    private Expression transformPostfixVariableExpression(final PostfixExpression exp, Expression operand, CompilerTransformer compiler) {
        VariableExpression ve = (VariableExpression) operand;
        if (ve.isThisExpression() || ve.isSuperExpression()) {
            compiler.addError("Prefix/Postfix operations can not be applied to 'this' and 'super'", exp);
            return null;
        }

        final org.codehaus.groovy.classgen.Variable var = compiler.compileStack.getVariable(ve.getName(), false);
        ClassNode vtype;
        if (var == null) {
            if (ve.isClosureSharedVariable()) {
                final PropertyExpression prop = new PropertyExpression(new VariableExpression("$self"), ve.getName());
                prop.setType(ve.getType());
                prop.setSourcePosition(exp);

                final PostfixExpression pe = new PostfixExpression(prop, exp.getOperation());
                pe.setSourcePosition(exp);

                return compiler.transform(pe);
            } else {
                final PropertyExpression prop = new PropertyExpression(VariableExpression.THIS_EXPRESSION, ve.getName());
                prop.setSourcePosition(exp);

                final PostfixExpression pe = new PostfixExpression(prop, exp.getOperation());
                pe.setSourcePosition(exp);

                return compiler.transform(pe);
            }
        }

        vtype = compiler.getLocalVarInferenceTypes().get(ve);
        if (vtype == null)
            vtype = var.getType();

        if (TypeUtil.isNumericalType(vtype)) {
            return new BytecodeExpr(exp, vtype) {
                protected void compile(MethodVisitor mv) {
                    final ClassNode primType = ClassHelper.getUnwrapper(getType());
                    load(getType(), var.getIndex(), mv);
                    dup(getType(), mv);
                    if (getType() != primType)
                        unbox(primType, mv);
                    incOrDecPrimitive(primType, exp.getOperation().getType(), mv);
                    if (getType() != primType)
                        box(primType, mv);
                    store(var, mv);
                }
            };
        }

        if (ClassHelper.isPrimitiveType(vtype))
            vtype = ClassHelper.getWrapper(vtype);

        final MethodNode methodNode = compiler.findMethod(vtype, "next", ClassNode.EMPTY_ARRAY);
        if (methodNode == null) {
            compiler.addError("Can't find method next() for type " + vtype.getName(), exp);
            return null;
        }

//        if (!TypeUtil.isDirectlyAssignableFrom(vtype, methodNode.getReturnType())) {
//            compiler.addError("Can't find method next() for type " + vtype.getName(), exp);
//            return null;
//        }

        final BytecodeExpr nextCall = (BytecodeExpr) compiler.transform(new MethodCallExpression(
                new BytecodeExpr(exp, vtype) {
                    protected void compile(MethodVisitor mv) {
                        load(var.getType(), var.getIndex(), mv);
                        dup(getType(), mv);
                    }
                },
                "next",
                new ArgumentListExpression()
        ));

        return new BytecodeExpr(exp, vtype) {
            protected void compile(MethodVisitor mv) {
                nextCall.visit(mv);
                store(var, mv);
            }
        };
    }
}
