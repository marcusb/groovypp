package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.PresentationUtil;
import org.mbte.groovypp.compiler.TypeUtil;
import org.objectweb.asm.MethodVisitor;

public class ResolvedArrayLikeBytecodeExpr extends ResolvedLeftExpr {
    private final BytecodeExpr array;
    private final BytecodeExpr index;
    private MethodNode getter;
    private final BytecodeExpr getterExpr;
    private final MethodNode setter;

    public ResolvedArrayLikeBytecodeExpr(ASTNode parent, BytecodeExpr array, BytecodeExpr index, MethodNode getter, CompilerTransformer compiler) {
        super(parent, getter.getReturnType());
        this.array = array;
        this.index = index;
        this.getter = getter;
        this.getterExpr = ResolvedMethodBytecodeExpr.create(parent, getter, array, new ArgumentListExpression(index), compiler);
        setType(getterExpr.getType());
        this.setter = compiler.findMethod(array.getType(), "putAt", new ClassNode[]{index.getType(), getType()}, false);
    }

    protected void compile(MethodVisitor mv) {
        getterExpr.visit(mv);
    }

    public BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right, final CompilerTransformer compiler) {
        if (!checkSetter(parent, compiler)) return null;

        if (setter.getReturnType().equals(ClassHelper.VOID_TYPE)) {
            final ClassNode type = setter.getParameters()[1].getType();
            Expression cast = new CastExpression(type, right);
            cast.setSourcePosition(right);
            final BytecodeExpr finalCast = (BytecodeExpr) compiler.transform(cast);

            final int [] v = new int [1];
            BytecodeExpr value = new BytecodeExpr(right, type) {
                protected void compile(MethodVisitor mv) {
                    finalCast.visit(mv);
                    dup(type, mv);
                    v [0] = compiler.compileStack.defineTemporaryVariable("$result", finalCast.getType(), true);
                }
            };

            final ResolvedMethodBytecodeExpr call = ResolvedMethodBytecodeExpr.create(parent, setter, array, new ArgumentListExpression(index, value), compiler);
            return new BytecodeExpr(parent, type) {
                protected void compile(MethodVisitor mv) {
                    call.visit(mv);
                    load(type, v[0], mv);
                    compiler.compileStack.removeVar(v[0]);
                }
            };
        }
        else {
            return ResolvedMethodBytecodeExpr.create(parent, setter, array, new ArgumentListExpression(index, right), compiler);
        }
    }

    private boolean checkSetter(ASTNode exp, CompilerTransformer compiler) {
        if (setter == null) {
            compiler.addError("Cannot find method 'putAt' for type: " + PresentationUtil.getText(getType()), exp);
            return false;
        }
        return true;
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, Token method, final BytecodeExpr right, CompilerTransformer compiler) {
        if (!checkSetter(parent, compiler)) return null;

        final BytecodeExpr loadArr = new BytecodeExpr(this, array.getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        final BytecodeExpr loadIndex = new BytecodeExpr(this, ClassHelper.int_TYPE) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        ResolvedMethodBytecodeExpr load  = ResolvedMethodBytecodeExpr.create(parent, getter, loadArr, new ArgumentListExpression(loadIndex), compiler);

        final BinaryExpression op = new BinaryExpression(load, method, right);
        op.setSourcePosition(parent);
        final BytecodeExpr transformedOp = compiler.cast((BytecodeExpr) compiler.transform(op), getType());

        final BytecodeExpr result = new BytecodeExpr(this, TypeUtil.wrapSafely(transformedOp.getType())) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        final ResolvedMethodBytecodeExpr store = ResolvedMethodBytecodeExpr.create(parent, setter, loadArr, new ArgumentListExpression(loadIndex, result), compiler);

        return new BytecodeExpr(parent, getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
                array.visit(mv);
                index.visit(mv);
                mv.visitInsn(DUP2);
                transformedOp.visit(mv);
                box(transformedOp.getType(), mv);
                dup_x2(getType(), mv);
                store.visit(mv);
                if (!setter.getReturnType().equals(ClassHelper.VOID_TYPE)) {
                    pop(setter.getReturnType(), mv);
                }
            }
        };
    }

    public BytecodeExpr createPrefixOp(ASTNode exp, final int type, CompilerTransformer compiler) {
        if (!checkSetter(exp, compiler)) return null;
        ClassNode vtype = getType();
        final BytecodeExpr incDec;
        if (TypeUtil.isNumericalType(vtype) && !vtype.equals(TypeUtil.Number_TYPE)) {
            incDec = new BytecodeExpr(exp, vtype) {
                protected void compile(MethodVisitor mv) {
                    final ClassNode primType = ClassHelper.getUnwrapper(getType());

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
                    new BytecodeExpr(exp, vtype) {
                        protected void compile(MethodVisitor mv) {
                        }
                    },
                    methodName,
                    new ArgumentListExpression()
            ));
        }

        final BytecodeExpr loadArr = new BytecodeExpr(this, array.getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        final BytecodeExpr loadIndex = new BytecodeExpr(this, ClassHelper.int_TYPE) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        final ResolvedMethodBytecodeExpr load  = ResolvedMethodBytecodeExpr.create(exp, getter, loadArr, new ArgumentListExpression(loadIndex), compiler);

        final BytecodeExpr result = new BytecodeExpr(this, TypeUtil.wrapSafely(incDec.getType())) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        final ResolvedMethodBytecodeExpr store = ResolvedMethodBytecodeExpr.create(exp, setter, loadArr, new ArgumentListExpression(loadIndex, result), compiler);

        return new BytecodeExpr(exp, getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
                array.visit(mv);
                index.visit(mv);
                mv.visitInsn(DUP2);

                load.visit(mv);

                incDec.visit(mv);

                dup_x2(getType(), mv);

                store.visit(mv);
            }
        };
    }

    public BytecodeExpr createPostfixOp(ASTNode exp, final int type, CompilerTransformer compiler) {
        if (!checkSetter(exp, compiler)) return null;

        ClassNode vtype = getType();
        final BytecodeExpr incDec;
        if (TypeUtil.isNumericalType(vtype) && !vtype.equals(TypeUtil.Number_TYPE)) {
            incDec = new BytecodeExpr(exp, vtype) {
                protected void compile(MethodVisitor mv) {
                    final ClassNode primType = ClassHelper.getUnwrapper(getType());

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
                    new BytecodeExpr(exp, vtype) {
                        protected void compile(MethodVisitor mv) {
                        }
                    },
                    methodName,
                    new ArgumentListExpression()
            ));
        }

        final BytecodeExpr loadArr = new BytecodeExpr(this, array.getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        final BytecodeExpr loadIndex = new BytecodeExpr(this, ClassHelper.int_TYPE) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        final ResolvedMethodBytecodeExpr load  = ResolvedMethodBytecodeExpr.create(exp, getter, loadArr, new ArgumentListExpression(loadIndex), compiler);

        final BytecodeExpr result = new BytecodeExpr(this, TypeUtil.wrapSafely(incDec.getType())) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        final ResolvedMethodBytecodeExpr store = ResolvedMethodBytecodeExpr.create(exp, setter, loadArr, new ArgumentListExpression(loadIndex, result), compiler);

        return new BytecodeExpr(exp, getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
                array.visit(mv);
                index.visit(mv);
                mv.visitInsn(DUP2);

                load.visit(mv);
                dup_x2(getType(), mv);

                incDec.visit(mv);

                store.visit(mv);
            }
        };
    }
}
