package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.AccessibilityCheck;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.PresentationUtil;
import org.mbte.groovypp.compiler.TypeUtil;
import org.objectweb.asm.MethodVisitor;

public class ResolvedFieldBytecodeExpr extends ResolvedLeftExpr {
    private final FieldNode fieldNode;
    private final BytecodeExpr object;
    private CompilerTransformer compiler;
    private ASTNode parent;
    private final BytecodeExpr value;

    public ResolvedFieldBytecodeExpr(ASTNode parent, FieldNode fieldNode, BytecodeExpr object, BytecodeExpr value, CompilerTransformer compiler) {
        super(parent, getType(object, fieldNode));
        this.parent = parent;
        this.fieldNode = fieldNode;
        this.object = object;
        this.compiler = compiler;
        this.value = value != null ? compiler.cast(value, fieldNode.getType() ): null;
    }

    private void checkAssignment() {
        if (fieldNode.isFinal() && !compiler.methodNode.getName().equals(fieldNode.isStatic() ? "<clinit>" : "<init>")) {
            compiler.addError("Cannot modify final field " + formatFieldName(), parent);
        }
    }

    public BytecodeExpr getObject() {
        return object;
    }

    public FieldNode getFieldNode() {
        return fieldNode;
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
        if (!AccessibilityCheck.isAccessible(fieldNode.getModifiers(), fieldNode.getDeclaringClass(),
                    compiler.classNode, object == null ? null : object.getType())) {
          compiler.addError("Cannot access field " + formatFieldName(), parent);
        }
        
        int op;
        if (object != null) {
            object.visit(mv);
            if (fieldNode.isStatic()) {
                pop(object.getType(), mv);
            } else {
                box(object.getType(), mv);
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

    public boolean isThis() {
        return object == null || object.isThis();
    }

    public BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right, CompilerTransformer compiler) {
        checkAssignment();
        String propName = fieldNode.getName();
        Object prop = PropertyUtil.resolveSetProperty(object != null ? object.getType() : fieldNode.getDeclaringClass(),
                propName, getType(), compiler, isThis());
        final CastExpression cast = new CastExpression(getType(), right);
        cast.setSourcePosition(right);
        right = (BytecodeExpr) compiler.transform(cast);
        return PropertyUtil.createSetProperty(parent, compiler, propName, object, right, prop);
    }

    public BytecodeExpr createBinopAssign(final ASTNode parent, Token method, BytecodeExpr right, final CompilerTransformer compiler) {
        checkAssignment();
        final BytecodeExpr fakeObject = object == null ? null : new BytecodeExpr(object, object.getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        BytecodeExpr get = new ResolvedFieldBytecodeExpr(this, fieldNode, fakeObject, null, compiler);

        final BinaryExpression op = new BinaryExpression(get, method, right);
        op.setSourcePosition(parent);
        final BytecodeExpr transformedOp = (BytecodeExpr) compiler.transform(op);

        Object prop = PropertyUtil.resolveSetProperty(object != null ? object.getType() : fieldNode.getDeclaringClass(),
                fieldNode.getName(), getType(), compiler, isThis());
        final CastExpression cast = new CastExpression(getType(), transformedOp);
        cast.setSourcePosition(right);
        right = (BytecodeExpr) compiler.transform(cast);
        final BytecodeExpr propExpr = PropertyUtil.createSetProperty(parent, compiler, fieldNode.getName(), fakeObject, right, prop);

        return object == null ? propExpr : new BytecodeExpr(parent, propExpr.getType()) {
            protected void compile(MethodVisitor mv) {
                object.visit(mv);
                mv.visitInsn(DUP);
                propExpr.visit(mv);
            }
        };
    }

    public BytecodeExpr createPrefixOp(ASTNode exp, final int type, CompilerTransformer compiler) {
        checkAssignment();
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

        Object prop = PropertyUtil.resolveSetProperty(object != null ? object.getType() : fieldNode.getDeclaringClass(),
                fieldNode.getName(), getType(), compiler, isThis());
        final CastExpression cast = new CastExpression(getType(), incDec);
        cast.setSourcePosition(exp);
        final BytecodeExpr put = PropertyUtil.createSetProperty(parent, compiler, fieldNode.getName(), dupObject, (BytecodeExpr) compiler.transform(cast), prop);

        return new BytecodeExpr(exp, getType()) {
            protected void compile(MethodVisitor mv) {
                put.visit(mv);
            }
        };
    }

    public BytecodeExpr createPostfixOp(ASTNode exp, final int type, CompilerTransformer compiler) {
        checkAssignment();
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
            final MethodNode methodNode = compiler.findMethod(vtype, methodName, ClassNode.EMPTY_ARRAY, false);
            if (methodNode == null) {
                compiler.addError("Cannot find method " + methodName + "() for type " + PresentationUtil.getText(vtype), exp);
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

        Object prop = PropertyUtil.resolveSetProperty(object != null ? object.getType() : fieldNode.getDeclaringClass(),
                fieldNode.getName(), getType(), compiler, isThis());
        final CastExpression cast = new CastExpression(getType(), incDec);
        cast.setSourcePosition(exp);
        final BytecodeExpr put = PropertyUtil.createSetProperty(parent, compiler, fieldNode.getName(), dupObject, (BytecodeExpr) compiler.transform(cast), prop);

        /*final BytecodeExpr put = new ResolvedFieldBytecodeExpr(
                exp,
                fieldNode,
                dupObject,
                incDec,
                compiler
        );*/

        return new BytecodeExpr(exp, getType()) {
            protected void compile(MethodVisitor mv) {
                put.visit(mv);
                pop(put.getType(), mv);
            }
        };
    }
}