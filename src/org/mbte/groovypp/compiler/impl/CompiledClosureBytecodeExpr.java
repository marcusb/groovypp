package org.mbte.groovypp.compiler.impl;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.impl.bytecode.BytecodeExpr;

import java.util.ArrayList;
import java.util.Iterator;

public class CompiledClosureBytecodeExpr extends BytecodeExpr {
    private final Parameter[] constrParams;
    private CompilerTransformer transformer;

    ClassNode delegateType;

    public CompiledClosureBytecodeExpr(CompilerTransformer compileExpressionTransformer, ClosureExpression ce, ClassNode newType, Parameter[] constrParams) {
        super(ce, newType);
        this.transformer = compileExpressionTransformer;
        setType(newType);
        this.constrParams = constrParams;
    }

    protected void compile() {
        final String classInternalName = BytecodeHelper.getClassInternalName(getType());
        mv.visitTypeInsn(NEW, classInternalName);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        for (int i = 1; i != constrParams.length; i++) {
            final String name = constrParams[i].getName();
            final org.codehaus.groovy.classgen.Variable var = transformer.compileStack.getVariable(name, false);
            if (var != null)
                mv.visitVarInsn(ALOAD, var.getIndex());
            else {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, BytecodeHelper.getClassInternalName(transformer.methodNode.getParameters()[0].getType()), name, "Lgroovy/lang/Reference;");
            }
        }
        mv.visitMethodInsn(INVOKESPECIAL, classInternalName, "<init>", BytecodeHelper.getMethodDescriptor(ClassHelper.VOID_TYPE, constrParams));
    }

    static Parameter[] createClosureParams(ClosureExpression ce, ClassNode newType) {
        ArrayList<FieldNode> refs = new ArrayList<FieldNode> ();
        for(Iterator it = ce.getVariableScope().getReferencedLocalVariablesIterator(); it.hasNext(); ) {
            Variable var = (Variable) it.next();
            final PropertyNode propertyNode = newType.addProperty(var.getName(), ACC_PUBLIC, ClassHelper.REFERENCE_TYPE, null, null, null);
            refs.add(propertyNode.getField());
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
        final Parameter[] constrParams = createClosureParams(ce, newType);
        return new CompiledClosureBytecodeExpr(transformer, ce, newType, constrParams);
    }
}
