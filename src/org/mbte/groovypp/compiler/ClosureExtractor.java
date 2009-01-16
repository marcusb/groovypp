package org.mbte.groovypp.compiler;

import groovy.lang.CompilePolicy;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.control.SourceUnit;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

class ClosureExtractor extends ClassCodeExpressionTransformer implements Opcodes{
    private final SourceUnit source;
    private final LinkedList toProcess;
    private final MethodNode methodNode;
    private final ClassNode classNode;
    private final CompilePolicy policy;

    public ClosureExtractor(SourceUnit source, LinkedList toProcess, MethodNode methodNode, ClassNode classNode, CompilePolicy policy) {
        this.source = source;
        this.toProcess = toProcess;
        this.methodNode = methodNode;
        this.classNode = classNode;
        this.policy = policy;
    }

    void extract (Statement code) {
        if (!methodNode.getName().equals("$doCall"))
           code.visit(this);
    }

    protected SourceUnit getSourceUnit() {
        return source;
    }

    public Expression transform(Expression exp) {
        if (exp instanceof ClosureExpression) {
            return transformClosure(exp);
        }

        return super.transform(exp);
    }

    private Expression transformClosure(Expression exp) {
        ClosureExpression ce = (ClosureExpression) exp;

        if (!ce.getType().equals(ClassHelper.CLOSURE_TYPE))
            return exp;

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

        final MethodNode _doCallMethod = methodNode.getDeclaringClass().addMethod(
                "$doCall",
                Opcodes.ACC_STATIC,
                ClassHelper.OBJECT_TYPE,
                newParams,
                ClassNode.EMPTY_ARRAY,
                ce.getCode());

        toProcess.add(_doCallMethod);
        toProcess.add(policy);


        pushState();
        ClosureExpression old = currentClosure;
        currentClosure = ce;

        for (Parameter pp : newParams)
            current.put(pp.getName (), new VarInfo(currentClosure, pp));

        ce.getCode().visit(this);

        currentClosure = old;
        popState();

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

        pushState();
        _doCallMethod.getCode().visit(this);
        popState();

        OpenVerifier v = new OpenVerifier();
        v.addDefaultParameterMethods(newType);

        for (Object o : newType.getMethods("doCall")) {
            MethodNode mn = (MethodNode) o;
            toProcess.add(mn);
            toProcess.add(policy);
        }

        newType.setModule(classNode.getModule());
        ce.setType(newType);

        return ce;
    }

    private static class VarScope extends HashMap<String, VarInfo> {
        private HashSet<VarInfo> externalRefs = new HashSet<VarInfo>();

        private HashSet<VarInfo> mutableRefs = new HashSet<VarInfo>();
        public VarScope parent;

        VarScope () {
            parent = null;
        }

        VarScope (VarScope parent) {
            putAll(parent);
            this.parent = parent;
        }
    }

    private LinkedList<VarScope> stack = new LinkedList<VarScope> ();

    private VarScope current = new VarScope();

    private ClosureExpression currentClosure;

    public void resolve(Statement code, Parameter p []) {
        pushState();
        for (Parameter pp : p) {
            current.put (pp.getName(), new VarInfo(currentClosure, pp));
        }
        code.visit(this);
        popState();
    }

    public void visitBlockStatement(BlockStatement block) {
        pushState();
        super.visitBlockStatement(block);
        popState();
    }

    public void visitCatchStatement(CatchStatement statement) {
        pushState();
        final Parameter p = statement.getVariable();
        current.put (p.getName(), new VarInfo(currentClosure, p));
        super.visitCatchStatement(statement);
        popState();
    }

    private void popState() {
        final VarScope scope = stack.removeLast();
        current = scope.parent;

        for (VarInfo vi : scope.externalRefs) {
            if (currentClosure != vi.closure) {
                current.externalRefs.add(vi);
            }
            else {
                vi.setClosureSharedVariable(true);
            }
        }

        for (VarInfo vi : scope.mutableRefs) {
            if (currentClosure != vi.closure) {
                current.mutableRefs.add(vi);
            }
        }
    }

    private void pushState() {
        final VarScope scope = new VarScope(current);
        stack.addLast(scope);
        current = scope;
    }

    public void visitForLoop(ForStatement forLoop) {
        pushState();
        Parameter p = forLoop.getVariable();
        if (p != ForStatement.FOR_LOOP_DUMMY)
            current.put (p.getName(), new VarInfo(currentClosure, p));
        super.visitForLoop(forLoop);
        popState();
    }
}
