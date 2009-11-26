package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.objectweb.asm.MethodVisitor;

public class ResolvedFieldBytecodeExpr extends ResolvedLeftExpr {
    private final FieldNode fieldNode;
    private final BytecodeExpr object;
    private final BytecodeExpr value;

    public ResolvedFieldBytecodeExpr(ASTNode parent, FieldNode fieldNode, BytecodeExpr object, BytecodeExpr value, CompilerTransformer compiler) {
        super(parent, getType(object, fieldNode));
        this.fieldNode = fieldNode;
        this.object = object;
        this.value = value != null ? compiler.cast(value, fieldNode.getType() ): null;

        if (fieldNode.isFinal() && value != null && !compiler.methodNode.getName().equals(fieldNode.isStatic() ? "<clinit>" : "<init>")) {
            compiler.addError("Can't modify final field " + formatFieldName(), parent);
        }

        checkFieldAccess(parent, fieldNode, compiler);
    }

    private void checkFieldAccess(ASTNode parent, FieldNode fieldNode, CompilerTransformer compiler) {
        if ((fieldNode.getModifiers() & ACC_PUBLIC) == 0) {
            if ((fieldNode.getModifiers() & ACC_PRIVATE) != 0) {
                if (!compiler.classNode.equals(fieldNode.getDeclaringClass())) {
                    compiler.addError("Can't access private field " + formatFieldName(), parent);
                }
            }

            if ((fieldNode.getModifiers() & ACC_PROTECTED) != 0) {
                if (!compiler.classNode.isDerivedFrom(fieldNode.getDeclaringClass())
                        && !compiler.samePackage(fieldNode)) {
                    compiler.addError("Can't access protected field " + formatFieldName(), parent);
                }
            }

            if ((fieldNode.getModifiers() & (ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE)) == 0) {
                if (!compiler.samePackage(fieldNode)) {
                    compiler.addError("Can't access package scoped field " + formatFieldName(), parent);
                }
            }
        }
    }

    private String formatFieldName() {
        return fieldNode.getDeclaringClass().getName() + "." + fieldNode.getName();
    }

    private static ClassNode getType(BytecodeExpr object, FieldNode fieldNode) {
        ClassNode type = fieldNode.getType();
        return object != null ? TypeUtil.getSubstitutedType(type,
                fieldNode.getDeclaringClass(), object.getType()) : type;
    }

    public void compile(MethodVisitor mv) {
        int op;
        if (object != null) {
            object.visit(mv);
            if (fieldNode.isStatic()) {
                pop(object.getType(), mv);
            } else {
                object.box(object.getType(), mv);
            }
        }

        if (value == null) {
            op = fieldNode.isStatic() ? GETSTATIC : GETFIELD;
        } else {
            op = fieldNode.isStatic() ? PUTSTATIC : PUTFIELD;
            value.visit(mv);

            if (object != null && !fieldNode.isStatic())
                dup_x1(value.getType(), mv);
            else
                dup(value.getType(), mv);
        }
        mv.visitFieldInsn(op, BytecodeHelper.getClassInternalName(fieldNode.getDeclaringClass()), fieldNode.getName(), BytecodeHelper.getTypeDescription(fieldNode.getType()));
        cast(TypeUtil.wrapSafely(fieldNode.getType()), TypeUtil.wrapSafely(getType()), mv);
    }

    public BytecodeExpr createAssign(ASTNode parent, Expression right, CompilerTransformer compiler) {
        return new ResolvedFieldBytecodeExpr(parent, fieldNode, object, (BytecodeExpr) compiler.transform(right), compiler);
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, Token method, final BytecodeExpr right, CompilerTransformer compiler) {
        final BytecodeExpr opLeft = new BytecodeExpr(this, getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };
        final BinaryExpression op = new BinaryExpression(opLeft, method, right);
        op.setSourcePosition(parent);
        final BytecodeExpr transformedOp = compiler.cast((BytecodeExpr) compiler.transform(op), getType());
        return new BytecodeExpr(parent, getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
                if (object != null) {
                    object.visit(mv);
                    if (fieldNode.isStatic()) {
                        pop(object.getType(), mv);
                    } else {
                        object.box(object.getType(), mv);
                        mv.visitInsn(DUP);
                    }
                }

                mv.visitFieldInsn(fieldNode.isStatic() ? GETSTATIC : GETFIELD, BytecodeHelper.getClassInternalName(fieldNode.getDeclaringClass()), fieldNode.getName(), BytecodeHelper.getTypeDescription(fieldNode.getType()));

                transformedOp.visit(mv);

                if (object != null)
                    dup_x1(getType(), mv);
                else
                    dup(getType(), mv);

                mv.visitFieldInsn(fieldNode.isStatic() ? PUTSTATIC : PUTFIELD, BytecodeHelper.getClassInternalName(fieldNode.getDeclaringClass()), fieldNode.getName(), BytecodeHelper.getTypeDescription(fieldNode.getType()));
            }
        };
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

        final BytecodeExpr get = new ResolvedFieldBytecodeExpr(
                exp,
                fieldNode,
                fakeObject,
                null,
                compiler
        );

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
                compiler.addError("Can't find method next() for type " + vtype.getName(), exp);
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

        final BytecodeExpr put = new ResolvedFieldBytecodeExpr(
                exp,
                fieldNode,
                dupObject,
                incDec,
                compiler
        );

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

        final BytecodeExpr get = new ResolvedFieldBytecodeExpr(
                exp,
                fieldNode,
                fakeObject,
                null,
                compiler
        );

        BytecodeExpr incDec;
        if (TypeUtil.isNumericalType(vtype) && !vtype.equals(TypeUtil.Number_TYPE)) {
            incDec = new BytecodeExpr(exp, vtype) {
                protected void compile(MethodVisitor mv) {
                    final ClassNode primType = ClassHelper.getUnwrapper(getType());

                    get.visit(mv);
                    if (ResolvedFieldBytecodeExpr.this.object != null && !fieldNode.isStatic())
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
                compiler.addError("Can't find method next() for type " + vtype.getName(), exp);
                return null;
            }

            incDec = (BytecodeExpr) compiler.transform(new MethodCallExpression(
                    new BytecodeExpr(exp, get.getType()) {
                        protected void compile(MethodVisitor mv) {
                            get.visit(mv);
                            if (ResolvedFieldBytecodeExpr.this.object != null && !fieldNode.isStatic())
                                dup_x1(get.getType(), mv);
                            else
                                dup(get.getType(), mv);
                        }
                    },
                    methodName,
                    new ArgumentListExpression()
            ));
        }

        final BytecodeExpr put = new ResolvedFieldBytecodeExpr(
                exp,
                fieldNode,
                dupObject,
                incDec,
                compiler
        );

        return new BytecodeExpr(exp, getType()) {
            protected void compile(MethodVisitor mv) {
                put.visit(mv);
                pop(put.getType(), mv);
            }
        };
    }
}