package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.Iterator;

public class CompiledClosureBytecodeExpr extends BytecodeExpr {
    private CompilerTransformer transformer;

    ClassNode delegateType;
    private final ClosureExpression ce;

    public CompiledClosureBytecodeExpr(CompilerTransformer compileExpressionTransformer, ClosureExpression ce, ClassNode newType) {
        super(ce, newType);
        this.transformer = compileExpressionTransformer;
        setType(newType);
        this.ce = ce;
    }

    protected void compile() {
        getType().getModule().addClass(getType());
        Parameter [] constrParams = createClosureParams(ce, getType());
        createClosureConstructor(getType(), constrParams);

        final String classInternalName = BytecodeHelper.getClassInternalName(getType());
        mv.visitTypeInsn(NEW, classInternalName);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        for (int i = 1; i != constrParams.length; i++) {
            final String name = constrParams[i].getName();
            final org.codehaus.groovy.classgen.Variable var = transformer.compileStack.getVariable(name, false);
            if (var != null) {
                loadVar(var);
                unbox(var.getType());
                if (!constrParams[i].getType().equals(var.getType())) {
                    mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(constrParams[i].getType()));
                }
            }
            else {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, BytecodeHelper.getClassInternalName(transformer.methodNode.getParameters()[0].getType()), name, BytecodeHelper.getTypeDescription(constrParams[i].getType()));
            }
        }
        mv.visitMethodInsn(INVOKESPECIAL, classInternalName, "<init>", BytecodeHelper.getMethodDescriptor(ClassHelper.VOID_TYPE, constrParams));
    }

    private Parameter[] createClosureParams(ClosureExpression ce, ClassNode newType) {
        ArrayList<FieldNode> refs = new ArrayList<FieldNode> ();
        for(Iterator it = ce.getVariableScope().getReferencedLocalVariablesIterator(); it.hasNext(); ) {

            Variable astVar = (Variable) it.next();
            final org.codehaus.groovy.classgen.Variable var = transformer.compileStack.getVariable(astVar.getName(), false);

            ClassNode vtype;
            if (var != null) {
                vtype = transformer.getLocalVarInferenceTypes().get(astVar);
                if (vtype == null)
                   vtype = var.getType();
            }
            else {
                vtype = transformer.methodNode.getParameters()[0].getType().getField(astVar.getName()).getType();
            }
            final FieldNode fieldNode = newType.addField(astVar.getName(), ACC_FINAL, vtype, null);
            refs.add(fieldNode);
        }

        final Parameter constrParams [] = new Parameter[refs.size()+1];
        constrParams [0] = new Parameter(ClassHelper.OBJECT_TYPE, "$owner");
        for (int i = 0; i != refs.size(); ++i) {
            final FieldNode fieldNode = refs.get(i);
            constrParams [i+1] = new Parameter(fieldNode.getType(), fieldNode.getName());
        }
        return constrParams;
    }

    public static Expression createCompiledClosureBytecodeExpr(final CompilerTransformer transformer, final ClosureExpression ce) {
        final ClassNode newType = ce.getType();
        return new CompiledClosureBytecodeExpr(transformer, ce, newType);
    }

    void createClosureConstructor(final ClassNode newType, final Parameter[] constrParams) {
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

                                final ClassNode type = constrParams[i].getType();
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
                                mv.visitFieldInsn(PUTFIELD, BytecodeHelper.getClassInternalName(newType), constrParams[i].getName(), BytecodeHelper.getTypeDescription(type));
                            }
                            mv.visitInsn(RETURN);
                        }
                }));
    }
}
