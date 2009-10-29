package org.mbte.groovypp.compiler;

import groovy.lang.TypePolicy;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.control.SourceUnit;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.LinkedList;

class ClosureExtractor extends ClassCodeExpressionTransformer implements Opcodes {
    private final SourceUnit source;
    private final LinkedList toProcess;
    private final MethodNode methodNode;
    private final ClassNode classNode;
    private final TypePolicy policy;

    private ClosureMethodNode currentClosureMethod;

    private int currentClosureIndex;
    private String currentClosureName;

    public ClosureExtractor(SourceUnit source, LinkedList toProcess, MethodNode methodNode, ClassNode classNode, TypePolicy policy) {
        this.source = source;
        this.toProcess = toProcess;
        this.methodNode = methodNode;
        this.classNode = classNode;
        this.policy = policy;
    }

    void extract(Statement code, String baseName) {
        currentClosureName = classNode.getName() + "$" + baseName.replace('<', '_').replace('>', '_');
        currentClosureIndex = 1;

        if (!methodNode.getName().equals("$doCall") && !methodNode.isAbstract())
            code.visit(this);
    }

    protected SourceUnit getSourceUnit() {
        return source;
    }

    public Expression transform(Expression exp) {
        if (exp instanceof ClosureExpression) {
            String oldCCN = currentClosureName;
            currentClosureName = currentClosureName + "_" + currentClosureIndex++;
            int oldCCI = currentClosureIndex;
            currentClosureIndex = 1;

            Expression res = transformClosure(exp);

            currentClosureIndex = oldCCI;
            currentClosureName = oldCCN;

            return res;
        }

        if (exp instanceof CastExpression) {
            CastExpression cast = (CastExpression) super.transform(exp);
            if (cast.getExpression() instanceof MapExpression && !cast.getType().implementsInterface(ClassHelper.MAP_TYPE)) {
            }
            return cast;
        }

        return super.transform(exp);
    }

    private Expression transformClosure(Expression exp) {
        ClosureExpression ce = (ClosureExpression) exp;

        if (!ce.getType().equals(ClassHelper.CLOSURE_TYPE))
            return exp;

        boolean addDefault = false;
        if (ce.getParameters() != null && ce.getParameters().length == 0) {
            addDefault = true;
            final VariableScope scope = ce.getVariableScope();
            ce = new ClosureExpression(new Parameter[1], ce.getCode());
            ce.setVariableScope(scope);
            ce.getParameters()[0] = new Parameter(ClassHelper.OBJECT_TYPE, "it", ConstantExpression.NULL);
        }

        final ClosureClassNode newType = new ClosureClassNode(classNode, currentClosureName);
        newType.setInterfaces(new ClassNode[]{TypeUtil.TCLOSURE});

        final Parameter[] newParams;
        if (ce.getParameters() != null) {
            newParams = new Parameter[ce.getParameters().length + 1];
            System.arraycopy(ce.getParameters(), 0, newParams, 1, ce.getParameters().length);
        } else {
            newParams = new Parameter[1];
        }
        newParams[0] = new Parameter(newType, "$self");

        final ClosureMethodNode _doCallMethod = new ClosureMethodNode(
                "$doCall",
                Opcodes.ACC_STATIC,
                ClassHelper.OBJECT_TYPE,
                newParams,
                ce.getCode());

        methodNode.getDeclaringClass().addMethod(_doCallMethod);
        newType.setDoCallMethod(_doCallMethod);
        if (currentClosureMethod != null)
            newType.addField("$owner", Opcodes.ACC_PUBLIC, currentClosureMethod.getParameters()[0].getType(), null);
        else
            if (!methodNode.isStatic())
                newType.addField("$owner", Opcodes.ACC_PUBLIC, classNode, null);

//        toProcess.add(_doCallMethod);
//        toProcess.add(policy);

        if (addDefault) {
            ClosureMethodNode _doCallMethodDef = new ClosureMethodNode.Dependent(
                    _doCallMethod,
                    "$doCall",
                    Opcodes.ACC_STATIC,
                    ClassHelper.OBJECT_TYPE,
                    new Parameter[] {new Parameter(newType, "$self")},
                    new BytecodeSequence(new BytecodeInstruction(){
                        public void visit(MethodVisitor mv) {
                            mv.visitVarInsn(ALOAD, 0);
                            final ClassNode type = _doCallMethod.getParameters()[1].getType();
                            if (ClassHelper.isPrimitiveType(type)) {
                                if (type == ClassHelper.long_TYPE) {
                                    mv.visitInsn(LCONST_0);
                                } else if (type == ClassHelper.double_TYPE) {
                                    mv.visitInsn(DCONST_0);
                                } else if (type == ClassHelper.float_TYPE) {
                                    mv.visitInsn(FCONST_0);
                                } else {
                                    mv.visitInsn(ICONST_0);
                                }
                            } else {
                                mv.visitInsn(ACONST_NULL);
                            }
                            mv.visitMethodInsn(INVOKESTATIC, BytecodeHelper.getClassInternalName(classNode), "$doCall", BytecodeHelper.getMethodDescriptor(_doCallMethod.getReturnType(),_doCallMethod.getParameters()));
                            mv.visitInsn(ARETURN);
                        }
                    })){
                @Override
                public ClassNode getReturnType() {
                    return _doCallMethod.getReturnType();
                }
            };

            methodNode.getDeclaringClass().addMethod(_doCallMethodDef);
        }

        _doCallMethod.setOwner(currentClosureMethod);

        ClosureMethodNode oldCmn = currentClosureMethod;
        currentClosureMethod = _doCallMethod;
        String oldCCN = currentClosureName;
        currentClosureName = currentClosureName + "_" + currentClosureIndex++;
        int oldCCI = currentClosureIndex;
        currentClosureIndex = 1;
        ce.getCode().visit(this);
        currentClosureIndex = oldCCI;
        currentClosureName = oldCCN;
        currentClosureMethod = oldCmn;

        createCallMethod(ce, _doCallMethod, "doCall");
//        createCallMethod(ce, _doCallMethod, "call");

        OpenVerifier v = new OpenVerifier();
        v.addDefaultParameterMethods(newType);

        newType.setModule(classNode.getModule());
        ce.setType(newType);

        return ce;
    }

    private void createCallMethod(ClosureExpression ce, final ClosureMethodNode method, final String name) {
        final Parameter[] newParams = method.getParameters();
        ClassNode newType = newParams[0].getType();
        newType.addMethod(
                name,
                Opcodes.ACC_PUBLIC,
                ClassHelper.OBJECT_TYPE,
                ce.getParameters() == null ? Parameter.EMPTY_ARRAY : ce.getParameters(),
                ClassNode.EMPTY_ARRAY,
                new BytecodeSequence(new BytecodeInstruction() {
                    public void visit(MethodVisitor mv) {
                        mv.visitVarInsn(ALOAD, 0);
                        for (int i = 1, k = 1; i != newParams.length; ++i) {
                            final ClassNode type = newParams[i].getType();
                            if (ClassHelper.isPrimitiveType(type)) {
                                if (type == ClassHelper.long_TYPE) {
                                    mv.visitVarInsn(LLOAD, k++);
                                    k++;
                                } else if (type == ClassHelper.double_TYPE) {
                                    mv.visitVarInsn(DLOAD, k++);
                                    k++;
                                } else if (type == ClassHelper.float_TYPE) {
                                    mv.visitVarInsn(FLOAD, k++);
                                } else {
                                    mv.visitVarInsn(ILOAD, k++);
                                }
                            } else {
                                mv.visitVarInsn(ALOAD, k++);
                            }
                        }
                        mv.visitMethodInsn(INVOKESTATIC, BytecodeHelper.getClassInternalName(classNode), "$doCall", BytecodeHelper.getMethodDescriptor(method.getReturnType(), newParams));
                        mv.visitInsn(ARETURN);
                    }
                }));
    }
}
