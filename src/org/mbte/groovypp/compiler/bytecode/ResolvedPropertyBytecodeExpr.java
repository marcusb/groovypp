package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.Verifier;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.PresentationUtil;
import org.mbte.groovypp.compiler.TypeUtil;
import org.objectweb.asm.MethodVisitor;

public class ResolvedPropertyBytecodeExpr extends ResolvedLeftExpr {
    private final PropertyNode propertyNode;
    private final BytecodeExpr object;
    private CompilerTransformer compiler;
    private final String methodName;
    private final BytecodeExpr bargs;

    public ResolvedPropertyBytecodeExpr(ASTNode parent, PropertyNode propertyNode, BytecodeExpr object, BytecodeExpr bargs, CompilerTransformer compiler) {
        super(parent, getType(object, propertyNode));
        this.propertyNode = propertyNode;
        this.object = object;
        this.compiler = compiler;
        this.bargs = bargs == null ? null : compiler.cast(bargs, getType());

        if (bargs != null) {
            methodName = "set" + Verifier.capitalize(propertyNode.getName());
        } else {
            methodName = "get" + Verifier.capitalize(propertyNode.getName());
        }
    }

    private static ClassNode getType(BytecodeExpr object, PropertyNode propertyNode) {
        ClassNode type = propertyNode.getType();
        return object != null ? TypeUtil.getSubstitutedType(type,
                propertyNode.getDeclaringClass(), object.getType()) : type;
    }

    public void compile(MethodVisitor mv) {
        final String classInternalName;
        final String methodDescriptor;

        int op = INVOKEVIRTUAL;

        if (propertyNode.getDeclaringClass().isInterface())
            op = INVOKEINTERFACE;

        if (propertyNode.isStatic())
            op = INVOKESTATIC;

        if (object != null) {
            object.visit(mv);
            box(object.getType(), mv);
        }

        if (propertyNode.isStatic() && object != null) {
            pop(object.getType(), mv);
        }

        classInternalName = BytecodeHelper.getClassInternalName(propertyNode.getDeclaringClass());

        if (methodName.startsWith("set")) {
            bargs.visit(mv);
            final ClassNode paramType = propertyNode.getType();
            final ClassNode type = bargs.getType();
            box(type, mv);
            cast(TypeUtil.wrapSafely(type), TypeUtil.wrapSafely(paramType), mv);
            unbox(paramType, mv);
            if (!propertyNode.isStatic())
                dup_x1(paramType, mv);
            else
                dup(paramType, mv);
            methodDescriptor = BytecodeHelper.getMethodDescriptor(ClassHelper.VOID_TYPE, new Parameter[]{new Parameter(paramType, "")});
            mv.visitMethodInsn(op, classInternalName, methodName, methodDescriptor);
        } else {
            if (propertyNode.getGetterBlock() == PropertyUtil.NO_CODE) {
                compiler.addError("Cannot find property '" + propertyNode.getName() + "'", this);
            }
            methodDescriptor = BytecodeHelper.getMethodDescriptor(propertyNode.getType(), Parameter.EMPTY_ARRAY);
            mv.visitMethodInsn(op, classInternalName, methodName, methodDescriptor);
            cast(TypeUtil.wrapSafely(propertyNode.getType()), TypeUtil.wrapSafely(getType()), mv);
        }
    }

    public BytecodeExpr createAssign(ASTNode parent, final BytecodeExpr right, CompilerTransformer compiler) {
        return new ResolvedPropertyBytecodeExpr(parent, propertyNode, object, right, compiler);
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, Token method, BytecodeExpr right, CompilerTransformer compiler) {
        final BytecodeExpr fakeObject = new BytecodeExpr(object, object.getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        final BytecodeExpr dupObject = new BytecodeExpr(object, object.getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
                object.visit(mv);
                dup(object.getType(), mv);
            }
        };

        BytecodeExpr get = new ResolvedPropertyBytecodeExpr(
                parent,
                propertyNode,
                dupObject,
                null,
                compiler);

        final BinaryExpression op = new BinaryExpression(get, method, right);
        op.setSourcePosition(parent);
        final BytecodeExpr transformedOp = (BytecodeExpr) compiler.transform(op);

        return new ResolvedPropertyBytecodeExpr(
                parent,
                propertyNode,
                fakeObject,
                transformedOp,
                compiler);
    }

    public BytecodeExpr createPrefixOp(ASTNode exp, final int type, CompilerTransformer compiler) {
        ClassNode vtype = getType();

        final BytecodeExpr fakeObject = new BytecodeExpr(object, object.getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        final BytecodeExpr dupObject = new BytecodeExpr(object, object.getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
                if (object != null) {
                    object.visit(mv);
                    dup(object.getType(), mv);
                }
            }
        };

        final BytecodeExpr get = new ResolvedPropertyBytecodeExpr(
                exp,
                propertyNode,
                fakeObject,
                null,
                compiler);

        BytecodeExpr incDec;
        if (TypeUtil.isNumericalType(vtype) && !vtype.equals(TypeUtil.Number_TYPE)) {
            incDec = new BytecodeExpr(exp, vtype) {
                protected void compile(MethodVisitor mv) {
                    final ClassNode primType = ClassHelper.getUnwrapper(getType());

                    get.visit(mv);

                    if (getType() != primType)
                        unbox(primType, mv);
                    incOrDecPrimitive(primType, type, mv);
                    if (getType() != primType)
                        box(primType, mv);
                }
            };
        }
        else {
            if (ClassHelper.isPrimitiveType(vtype))
                vtype = TypeUtil.wrapSafely(vtype);

            String methodName = type == Types.PLUS_PLUS ? "next" : "previous";
            final MethodNode methodNode = compiler.findMethod(vtype, methodName, ClassNode.EMPTY_ARRAY, false);
            if (methodNode == null) {
                compiler.addError("Cannot find method " + methodName + "() for type " + PresentationUtil.getText(vtype), exp);
                return null;
            }

            incDec = (BytecodeExpr) compiler.transform(new MethodCallExpression(
                    new BytecodeExpr(exp, get.getType()) {
                        protected void compile(MethodVisitor mv) {
                            get.visit(mv);
                        }
                    },
                    methodName,
                    new ArgumentListExpression()
            ));
        }

        final BytecodeExpr put = new ResolvedPropertyBytecodeExpr(
                exp,
                propertyNode,
                dupObject,
                incDec,
                compiler);

        return new BytecodeExpr(exp, getType()) {
            protected void compile(MethodVisitor mv) {
                put.visit(mv);
            }
        };
    }

    public BytecodeExpr createPostfixOp(ASTNode exp, final int type, CompilerTransformer compiler) {
        ClassNode vtype = getType();

        final BytecodeExpr fakeObject = new BytecodeExpr(object, object.getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        final BytecodeExpr dupObject = new BytecodeExpr(object, object.getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
                if (object != null) {
                    object.visit(mv);
                    dup(object.getType(), mv);
                }
            }
        };

        final BytecodeExpr get = new ResolvedPropertyBytecodeExpr(
                exp,
                propertyNode,
                fakeObject,
                null,
                compiler);

        BytecodeExpr incDec;
        if (TypeUtil.isNumericalType(vtype) && !vtype.equals(TypeUtil.Number_TYPE)) {
            incDec = new BytecodeExpr(exp, vtype) {
                protected void compile(MethodVisitor mv) {
                    final ClassNode primType = ClassHelper.getUnwrapper(getType());

                    get.visit(mv);
                    if (ResolvedPropertyBytecodeExpr.this.object != null && !propertyNode.isStatic())
                        dup_x1(get.getType(), mv);
                    else
                        dup(get.getType(), mv);

                    if (getType() != primType)
                        unbox(primType, mv);
                    incOrDecPrimitive(primType, type, mv);
                    if (getType() != primType)
                        box(primType, mv);
                }
            };
        }
        else {
            if (ClassHelper.isPrimitiveType(vtype))
                vtype = TypeUtil.wrapSafely(vtype);

            String methodName = type == Types.PLUS_PLUS ? "next" : "previous";
            final MethodNode methodNode = compiler.findMethod(vtype, methodName, ClassNode.EMPTY_ARRAY, false);
            if (methodNode == null) {
                compiler.addError("Cannot find method " + methodName + "() for type " + PresentationUtil.getText(vtype), exp);
                return null;
            }

            incDec = (BytecodeExpr) compiler.transform(new MethodCallExpression(
                    new BytecodeExpr(exp, get.getType()) {
                        protected void compile(MethodVisitor mv) {
                            get.visit(mv);
                            if (ResolvedPropertyBytecodeExpr.this.object != null && !propertyNode.isStatic())
                                dup_x1(get.getType(), mv);
                            else
                                dup(get.getType(), mv);
                        }
                    },
                    methodName,
                    new ArgumentListExpression()
            ));
        }

        final BytecodeExpr put = new ResolvedPropertyBytecodeExpr(
                exp,
                propertyNode,
                dupObject,
                incDec,
                compiler);

        return new BytecodeExpr(exp, getType()) {
            protected void compile(MethodVisitor mv) {
                put.visit(mv);
                pop(put.getType(), mv);
            }
        };
    }
}
