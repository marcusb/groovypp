package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.PresentationUtil;
import org.mbte.groovypp.compiler.transformers.VariableExpressionTransformer;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ResolvedGetterBytecodeExpr extends ResolvedLeftExpr {
    private final MethodNode methodNode;

    private final BytecodeExpr object;
    private final boolean needsObjectIfStatic;
    private CompilerTransformer compiler;
    private String propName;
    private final BytecodeExpr getter;
    private static final ArgumentListExpression EMPTY_ARGS = new ArgumentListExpression();

    public ResolvedGetterBytecodeExpr(ASTNode parent, MethodNode methodNode, BytecodeExpr object, boolean needsObjectIfStatic, CompilerTransformer compiler, String propName) {
        super(parent, ResolvedMethodBytecodeExpr.getReturnType(methodNode, object, EMPTY_ARGS, compiler));
        this.methodNode = methodNode;
        this.object = object;
        this.needsObjectIfStatic = needsObjectIfStatic;
        this.compiler = compiler;
        this.propName = propName;
        getter = ResolvedMethodBytecodeExpr.create(
                parent,
                methodNode,
                methodNode.isStatic() && !needsObjectIfStatic ? null : object,
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
        String name = methodNode.getName().substring(3);
        name = name.substring(0, 1).toLowerCase() + name.substring(1);
        Object prop = PropertyUtil.resolveSetProperty(object != null ? object.getType() : methodNode.getDeclaringClass(),
                name, right.getType(), compiler, isThisCall());
        return PropertyUtil.createSetProperty(parent, compiler, name, object, right, prop);
    }

    private boolean isThisCall() {
        return object == null || object instanceof VariableExpressionTransformer.This;
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, Token method, BytecodeExpr right, CompilerTransformer compiler) {
        String name = methodNode.getName().substring(3);
        name = name.substring(0, 1).toLowerCase() + name.substring(1);

        final BytecodeExpr fakeObject = new BytecodeExpr(object, object.getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        BytecodeExpr get = ResolvedMethodBytecodeExpr.create(
                parent,
                methodNode,
                methodNode.isStatic() && !needsObjectIfStatic ? null : fakeObject,
                EMPTY_ARGS, compiler);

        final BinaryExpression op = new BinaryExpression(get, method, right);
        op.setSourcePosition(parent);
        final BytecodeExpr transformedOp = (BytecodeExpr) compiler.transform(op);

        Object prop = PropertyUtil.resolveSetProperty(object.getType(), name, transformedOp.getType(), compiler,
                isThisCall());
        final BytecodeExpr propExpr = PropertyUtil.createSetProperty(parent, compiler, name, fakeObject, transformedOp, prop);

        return new BytecodeExpr(parent, propExpr.getType()) {
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
                needsObjectIfStatic,
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
            final MethodNode methodNode = compiler.findMethod(vtype, methodName, ClassNode.EMPTY_ARRAY);
            if (methodNode == null) {
                compiler.addError("Can't find method next() for type " + PresentationUtil.getText(vtype), exp);
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
                needsObjectIfStatic,
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
            final MethodNode methodNode = compiler.findMethod(vtype, methodName, ClassNode.EMPTY_ARRAY);
            if (methodNode == null) {
                compiler.addError("Can't find method next() for type " + PresentationUtil.getText(vtype), exp);
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

        public Accessor(FieldNode fieldNode, ASTNode parent, MethodNode methodNode, BytecodeExpr object, boolean needsObjectIfStatic, CompilerTransformer compiler) {
            super(parent, methodNode, object, needsObjectIfStatic, compiler, fieldNode.getName());
            this.fieldNode = fieldNode;
        }

        public FieldNode getFieldNode() {
            return fieldNode;
        }
    }
}