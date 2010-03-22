package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.PresentationUtil;
import org.mbte.groovypp.compiler.TypeUtil;
import org.objectweb.asm.MethodVisitor;

public class ResolvedGetterBytecodeExpr extends ResolvedLeftExpr {
    private final MethodNode methodNode;

    private final BytecodeExpr object;
    private CompilerTransformer compiler;
    private String propName;
    private final BytecodeExpr getter;
    private static final ArgumentListExpression EMPTY_ARGS = new ArgumentListExpression();

    public ResolvedGetterBytecodeExpr(ASTNode parent, MethodNode methodNode, BytecodeExpr object, CompilerTransformer compiler, String propName) {
        super(parent, ResolvedMethodBytecodeExpr.getReturnType(methodNode, object, EMPTY_ARGS, compiler));
        this.methodNode = methodNode;
        this.object = object;
        this.compiler = compiler;
        this.propName = propName;
        getter = ResolvedMethodBytecodeExpr.create(
                parent,
                methodNode,
                object,
                EMPTY_ARGS, compiler);
        setType(getter.getType());
    }

    public FieldNode getFieldNode() {
        return compiler.findField(methodNode.getDeclaringClass(), propName);
    }

    protected void compile(MethodVisitor mv) {
        getter.visit(mv);
    }

    public BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right, CompilerTransformer compiler) {
        Object prop = PropertyUtil.resolveSetProperty(object != null ? object.getType() : methodNode.getDeclaringClass(),
                propName, getType(), compiler, isThisCall());
        final CastExpression cast = new CastExpression(getType(), right);
        cast.setSourcePosition(right);
        right = (BytecodeExpr) compiler.transform(cast);
        return PropertyUtil.createSetProperty(parent, compiler, propName, object, right, prop);
    }

    private boolean isThisCall() {
        return object == null || object.isThis();
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, Token method, BytecodeExpr right, CompilerTransformer compiler) {
        final BytecodeExpr fakeObject = object == null ? null : new BytecodeExpr(object, object.getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        BytecodeExpr get = ResolvedMethodBytecodeExpr.create(
                parent,
                methodNode,
                fakeObject,
                EMPTY_ARGS, compiler);

        final BinaryExpression op = new BinaryExpression(get, method, right);
        op.setSourcePosition(parent);
        final BytecodeExpr transformedOp = (BytecodeExpr) compiler.transform(op);

        Object prop = PropertyUtil.resolveSetProperty(methodNode.getDeclaringClass(), propName, transformedOp.getType(), compiler,
                isThisCall());
        final BytecodeExpr propExpr = PropertyUtil.createSetProperty(parent, compiler, propName, fakeObject, transformedOp, prop);

        return object == null ? propExpr : new BytecodeExpr(parent, propExpr.getType()) {
            protected void compile(MethodVisitor mv) {
                object.visit(mv);
                mv.visitInsn(DUP);
                propExpr.visit(mv);
            }
        };
    }

    public BytecodeExpr createPrefixOp(ASTNode exp, final int type, CompilerTransformer compiler) {
        ClassNode vtype = getType();

        final BytecodeExpr fakeObject = object == null ? null : new BytecodeExpr(object, object.getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        final BytecodeExpr dupObject = object == null ? null : new BytecodeExpr(object, object.getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
                object.visit(mv);
                dup(object.getType(), mv);
            }
        };

        final BytecodeExpr get = new ResolvedGetterBytecodeExpr(
                exp,
                methodNode,
                fakeObject,
                compiler, propName);

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

        Object prop = PropertyUtil.resolveSetProperty(methodNode.getDeclaringClass(), propName, incDec.getType(), compiler,
                isThisCall());
        return PropertyUtil.createSetProperty(exp, compiler, propName, dupObject, incDec, prop);
    }

    public BytecodeExpr createPostfixOp(ASTNode exp, final int type, CompilerTransformer compiler) {
        ClassNode vtype = getType();

        final BytecodeExpr fakeObject = object == null ? null : new BytecodeExpr(object, object.getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        final BytecodeExpr dupObject = object == null ? null : new BytecodeExpr(object, object.getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
                object.visit(mv);
                dup(object.getType(), mv);
            }
        };

        final BytecodeExpr get = new ResolvedGetterBytecodeExpr(
                exp,
                methodNode,
                fakeObject,
                compiler, propName);

        BytecodeExpr incDec;
        if (TypeUtil.isNumericalType(vtype) && !vtype.equals(TypeUtil.Number_TYPE)) {
            incDec = new BytecodeExpr(exp, vtype) {
                protected void compile(MethodVisitor mv) {
                    final ClassNode primType = ClassHelper.getUnwrapper(getType());

                    get.visit(mv);
                    if (object != null && !methodNode.isStatic())
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
                            if (object != null && !ResolvedGetterBytecodeExpr.this.methodNode.isStatic())
                                dup_x1(get.getType(), mv);
                            else
                                dup(get.getType(), mv);
                        }
                    },
                    methodName,
                    new ArgumentListExpression()
            ));
        }

        Object prop = PropertyUtil.resolveSetProperty(methodNode.getDeclaringClass(), propName, incDec.getType(), compiler,
                isThisCall());

        final BytecodeExpr put = PropertyUtil.createSetProperty(exp, compiler, propName, dupObject, incDec, prop);
        return new BytecodeExpr(exp, getType()) {
            protected void compile(MethodVisitor mv) {
                put.visit(mv);
                pop(put.getType(), mv);
            }
        };
    }

    public BytecodeExpr getObject() {
        return object;
    }

    public static class Accessor extends ResolvedGetterBytecodeExpr {
        private FieldNode fieldNode;

        public Accessor(FieldNode fieldNode, ASTNode parent, MethodNode methodNode, BytecodeExpr object, CompilerTransformer compiler) {
            super(parent, methodNode, object, compiler, fieldNode.getName());
            this.fieldNode = fieldNode;
        }

        public FieldNode getFieldNode() {
            return fieldNode;
        }
    }
}