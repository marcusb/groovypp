package org.mbte.groovypp.compiler.impl;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.LinkedList;
import java.util.Iterator;

class ClosureExtractor extends ClassCodeExpressionTransformer implements Opcodes{
    private final SourceUnit source;
    private final LinkedList<MethodNode> toProcess;
    private final MethodNode methodNode;
    private final ClassNode classNode;

    public ClosureExtractor(SourceUnit source, LinkedList<MethodNode> toProcess, MethodNode methodNode, ClassNode classNode) {
        this.source = source;
        this.toProcess = toProcess;
        this.methodNode = methodNode;
        this.classNode = classNode;
    }

    protected SourceUnit getSourceUnit() {
        return source;
    }

    @Override
    public Expression transform(Expression exp) {
        if (exp instanceof ClosureExpression) {
            ClosureExpression ce = (ClosureExpression) exp;

            if (ce.getParameters() != null && ce.getParameters().length == 0) {
                final VariableScope scope = ce.getVariableScope();
                ce = new ClosureExpression(new Parameter[1], ce.getCode());
                ce.setVariableScope(scope);
                ce.getParameters()[0] = new Parameter(ClassHelper.OBJECT_TYPE, "it");
                ce.getParameters()[0].setInitialExpression(ConstantExpression.NULL);
            }

            final ClassNode newType = new ClassNode(
                    classNode.getName() + "$" + System.identityHashCode(ce),
                    0,
                    ClassHelper.CLOSURE_TYPE);
            newType.setInterfaces(new ClassNode[]{TypeUtil.TCLOSURE});

            final Parameter[] newParams;
            if (ce.getParameters() != null) {
                newParams = new Parameter[ce.getParameters().length+1];
                System.arraycopy(ce.getParameters(), 0, newParams, 1, ce.getParameters().length);
            }
            else {
                newParams = new Parameter[1];
            }
            newParams[0] = new Parameter(newType, "$self");

            final Parameter[] constrParams = CompiledClosureBytecodeExpr.createClosureParams(ce, newType);

            final MethodNode newMethod = methodNode.getDeclaringClass().addMethod(
                    "$doCall",
                    Opcodes.ACC_STATIC,
                    ClassHelper.OBJECT_TYPE,
                    newParams,
                    ClassNode.EMPTY_ARRAY,
                    ce.getCode());
            toProcess.add(newMethod);

            newType.addMethod(
                    "doCall",
                    Opcodes.ACC_PROTECTED,
                    ClassHelper.OBJECT_TYPE,
                    ce.getParameters() == null ? Parameter.EMPTY_ARRAY : ce.getParameters(),
                    ClassNode.EMPTY_ARRAY,
                    new BytecodeSequence(new BytecodeInstruction(){
                        public void visit(MethodVisitor mv) {
                            mv.visitVarInsn(ALOAD, 0);
                            for (int i = 1, k = 1; i != newParams.length; ++i) {
                                final ClassNode type = newParams[i].getType();
                                if (ClassHelper.isPrimitiveType(type)) {
                                    if (type == ClassHelper.long_TYPE) {
                                        mv.visitVarInsn(LLOAD, k++);
                                        k++;
                                    }
                                    else if (type == ClassHelper.double_TYPE) {
                                        mv.visitVarInsn(DLOAD, k++);
                                        k++;
                                    }
                                    else if (type == ClassHelper.float_TYPE) {
                                        mv.visitVarInsn(FLOAD, k++);
                                    }
                                    else {
                                        mv.visitVarInsn(ILOAD, k++);
                                    }
                                }
                                else {
                                    mv.visitVarInsn(ALOAD, k++);
                                }
                            }
                            mv.visitMethodInsn(INVOKESTATIC, BytecodeHelper.getClassInternalName(classNode), "$doCall", BytecodeHelper.getMethodDescriptor(ClassHelper.OBJECT_TYPE, newParams));
                            mv.visitInsn(ARETURN);
                        }
                    }));

            newType.addMethod(
                    "<init>",
                    ACC_PUBLIC,
                    ClassHelper.VOID_TYPE,
                    constrParams,
                    ClassNode.EMPTY_ARRAY,
                    new BytecodeSequence(new BytecodeInstruction(){
                        public void visit(MethodVisitor mv) {
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitVarInsn(ALOAD, 1);
                            mv.visitMethodInsn(INVOKESPECIAL, BytecodeHelper.getClassInternalName(newType.getSuperClass()), "<init>", "(Ljava/lang/Object;)V");

                            for (int i = 1, k = 2; i != constrParams.length; i++) {
                                mv.visitVarInsn(ALOAD, 0);
                                mv.visitVarInsn(ALOAD, k++);
                                mv.visitFieldInsn(PUTFIELD, BytecodeHelper.getClassInternalName(newType), constrParams[i].getName(), "Lgroovy/lang/Reference;");
                            }
                        }
                    }));

            OpenVerifier v = new OpenVerifier();
            v.addDefaultParameterMethods(newType);

            for (Iterator it = newType.getMethods("doCall").iterator(); it.hasNext(); ) {
                MethodNode mn = (MethodNode) it.next();
                toProcess.add(mn);
            }

            classNode.getModule().addClass(newType);
            ce.setType(newType);

            return ce;
        }

        return super.transform(exp);
    }

}
