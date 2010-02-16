package org.mbte.groovypp.compiler.transformers;

import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.objectweb.asm.MethodVisitor;

class InnerThisBytecodeExpr extends BytecodeExpr {
    private final ClassNode thisTypeFinal;
    private final CompilerTransformer compiler;

    public InnerThisBytecodeExpr(Expression exp, ClassNode thisTypeFinal, CompilerTransformer compiler) {
        super(exp, thisTypeFinal);
        this.thisTypeFinal = thisTypeFinal;
        this.compiler = compiler;
    }

    protected void compile(MethodVisitor mv) {
        mv.visitVarInsn(ALOAD, 0);
        ClassNode curThis = compiler.methodNode.getDeclaringClass();
        while (curThis != thisTypeFinal) {
            compiler.context.setOuterClassInstanceUsed(curThis);
            ClassNode next = curThis.getField("this$0").getType();
            mv.visitFieldInsn(GETFIELD, BytecodeHelper.getClassInternalName(curThis), "this$0", BytecodeHelper.getTypeDescription(next));
            curThis = next;
        }
    }
}
