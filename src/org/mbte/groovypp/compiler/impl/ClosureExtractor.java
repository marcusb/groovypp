package org.mbte.groovypp.compiler.impl;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.syntax.Types;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;

import groovy.lang.CompilePolicy;

class ClosureExtractor extends ClassCodeExpressionTransformer implements Opcodes{
    private final SourceUnit source;
    private final LinkedList toProcess;
    private final MethodNode methodNode;
    private final ClassNode classNode;
    private final CompilePolicy policy;
    private Parameter self;

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
//        if (exp instanceof PrefixExpression) {
//            return transformPrefixExpression((PrefixExpression) exp);
//        }
//        if (exp instanceof PostfixExpression) {
//            return transformPostfixExpression((PostfixExpression) exp);
//        }
//        if (exp instanceof VariableExpression) {
//            return transformVariableExpression((VariableExpression) exp);
//        }
//        if (exp instanceof BinaryExpression) {
//            if (exp instanceof DeclarationExpression)
//                return transformDeclarationExpression((DeclarationExpression) exp);
//            else
//                return transformBinaryExpression((BinaryExpression) exp);
//        }
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

        self = newParams[0];
        ce.getCode().visit(this);
        self = null;

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

        final Parameter[] constrParams = CompiledClosureBytecodeExpr.createClosureParams(ce, newType);

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
                            mv.visitInsn(RETURN);
                        }
                }));

        OpenVerifier v = new OpenVerifier();
        v.addDefaultParameterMethods(newType);

        for (Iterator it = newType.getMethods("doCall").iterator(); it.hasNext(); ) {
            MethodNode mn = (MethodNode) it.next();
            toProcess.add(mn);
            toProcess.add(policy);
        }

        classNode.getModule().addClass(newType);
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
        final VarScope scope = stack.pop();
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
        stack.push(scope);
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

    private Expression transformBinaryExpression(BinaryExpression expression) {
        if(expression.getLeftExpression() instanceof VariableExpression) {
            switch (expression.getOperation().getType()) {
                case Types.EQUAL: // = assignment
                case Types.BITWISE_AND_EQUAL:
                case Types.BITWISE_OR_EQUAL:
                case Types.BITWISE_XOR_EQUAL:
                case Types.PLUS_EQUAL:
                case Types.MINUS_EQUAL:
                case Types.MULTIPLY_EQUAL:
                case Types.DIVIDE_EQUAL:
                case Types.INTDIV_EQUAL:
                case Types.MOD_EQUAL:
                case Types.POWER_EQUAL:
                case Types.LEFT_SHIFT_EQUAL:
                case Types.RIGHT_SHIFT_EQUAL:
                case Types.RIGHT_SHIFT_UNSIGNED_EQUAL:
                    if (expression.getLeftExpression() == VariableExpression.THIS_EXPRESSION
                            || expression.getLeftExpression() == VariableExpression.SUPER_EXPRESSION) {

                    }
                    else {
                        final VarInfo varInfo = current.get(((VariableExpression)expression.getLeftExpression()).getName());
                        if(varInfo.closure != currentClosure) {
                            current.mutableRefs.add(varInfo);
                        }
                    }
                    break;

                default:
            }
        }

        return super.transform(expression);
    }

    private Expression transformPostfixExpression(PostfixExpression expression) {
        if(expression.getExpression() instanceof VariableExpression) {
            // if in closure and external then set mutable in closure
            if (expression.getExpression() == VariableExpression.THIS_EXPRESSION
                    || expression.getExpression() == VariableExpression.SUPER_EXPRESSION) {

            }
            else {
                final VarInfo varInfo = current.get(((VariableExpression)expression.getExpression()).getName());
                if(varInfo.closure != currentClosure) {
                    current.mutableRefs.add(varInfo);
                }
            }
        }

        return super.transform(expression);
    }

    private Expression transformPrefixExpression(PrefixExpression expression) {
        if(expression.getExpression() instanceof VariableExpression) {
            // if in closure and external then set mutable in closure
            if (expression.getExpression() == VariableExpression.THIS_EXPRESSION
                    || expression.getExpression() == VariableExpression.SUPER_EXPRESSION) {

            }
            else {
                final VarInfo varInfo = current.get(((VariableExpression)expression.getExpression()).getName());
                if(varInfo.closure != currentClosure) {
                    current.mutableRefs.add(varInfo);
                }
            }
        }

        return super.transform(expression);
    }

    private Expression transformVariableExpression(VariableExpression expression) {
        // if external in closure mark as accessed
        if (expression == VariableExpression.THIS_EXPRESSION || expression == VariableExpression.SUPER_EXPRESSION) {
            return expression;
        }
        else {
            final VarInfo varInfo = current.get(expression.getName());
//            expression.setAccessedVariable(varInfo);
            if(varInfo.closure != currentClosure) {
                current.externalRefs.add(varInfo);
                return new PropertyExpression(transformVariableExpression(new VariableExpression(self)), varInfo.getName());
            }
            return expression;
        }
    }

    private Expression transformDeclarationExpression(DeclarationExpression expression) {
        final VariableExpression ve = expression.getVariableExpression();
        current.put(ve.getName(), new VarInfo(currentClosure, ve));

        return super.transform(expression);
    }
}
