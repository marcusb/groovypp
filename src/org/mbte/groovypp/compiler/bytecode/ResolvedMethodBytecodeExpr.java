package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.ClassNodeCache;
import org.mbte.groovypp.compiler.CompiledClosureBytecodeExpr;
import org.mbte.groovypp.compiler.TypeUtil;

public class ResolvedMethodBytecodeExpr extends BytecodeExpr {
    private final MethodNode methodNode;
    private final BytecodeExpr object;
    private final String methodName;
    private final ArgumentListExpression bargs;

    public ResolvedMethodBytecodeExpr(Expression parent, MethodNode methodNode, BytecodeExpr object, ArgumentListExpression bargs) {
        super (parent, methodNode.getReturnType());
        this.methodNode = methodNode;
        this.object = object;
        this.methodName = methodNode.getName();
        this.bargs = bargs;

        tryImproveClosureType(methodNode, bargs);
    }

    private void tryImproveClosureType(MethodNode methodNode, ArgumentListExpression bargs) {
        final Parameter[] parameters = methodNode.getParameters();
        if (parameters.length > 0) {
            final Parameter lastParam = parameters[parameters.length - 1];
            if (lastParam.getType().getName().equals(TypeUtil.TCLOSURE.getName())) {
                if (lastParam.getType().isUsingGenerics()) {
                    final GenericsType[] genericsTypes = lastParam.getType().getGenericsTypes();
                    if (genericsTypes != null) {
                        Object param = bargs.getExpressions().get(parameters.length - 1);
                        if (param instanceof CompiledClosureBytecodeExpr) {
                            CompiledClosureBytecodeExpr ce = (CompiledClosureBytecodeExpr) param;
                            ce.getType().getInterfaces()[0].setGenericsTypes(new GenericsType[]{new GenericsType(genericsTypes[0].getType())});
                        }
                    }
                }
            }
        }
    }

    public void compile() {
        int op = INVOKEVIRTUAL;
        final String classInternalName;
        final String methodDescriptor;
        if (methodNode instanceof ClassNodeCache.DGM) {
            op = INVOKESTATIC;

            if (object != null) {
                object.visit(mv);
                box(object.getType());
                if (methodNode.getDeclaringClass() != ClassHelper.OBJECT_TYPE);
                   mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(methodNode.getDeclaringClass()));
            }

            classInternalName = BytecodeHelper.getClassInternalName(((ClassNodeCache.DGM)methodNode).callClass);
            methodDescriptor = ((ClassNodeCache.DGM)methodNode).descr;
        }
        else {
            if (methodNode.isStatic())
              op = INVOKESTATIC;
            else
            if (methodNode.getDeclaringClass().isInterface())
              op = INVOKEINTERFACE;

            if (object != null) {
                object.visit(mv);
                box(object.getType());
            }

            if (op == INVOKESTATIC && object != null) {
                if (ClassHelper.long_TYPE == object.getType() || ClassHelper.double_TYPE == object.getType())
                    mv.visitInsn(POP2);
                else
                    mv.visitInsn(POP);
            }
            classInternalName = BytecodeHelper.getClassInternalName(methodNode.getDeclaringClass());
            methodDescriptor = BytecodeHelper.getMethodDescriptor(methodNode.getReturnType(), methodNode.getParameters());
        }

        for (int i = 0; i != bargs.getExpressions().size(); ++i) {
            BytecodeExpr be = (BytecodeExpr) bargs.getExpressions().get(i);
            be.visit(mv);
            final ClassNode paramType = methodNode.getParameters()[i].getType();
            final ClassNode type = be.getType();
            box(type);
            be.cast(ClassHelper.getWrapper(type), ClassHelper.getWrapper(paramType));
            be.unbox(paramType);
        }
        mv.visitMethodInsn(op, classInternalName, methodName, methodDescriptor);
    }
}
