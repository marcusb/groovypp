package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.objectweb.asm.MethodVisitor;

public class InnerThisBytecodeExpr extends BytecodeExpr {
    private final ClassNode innerClass;
    private final ClassNode outerClass;
    private final CompilerTransformer compiler;

    public InnerThisBytecodeExpr(ASTNode parent, ClassNode outerClass, CompilerTransformer compiler) {
        this(parent, outerClass, compiler, compiler.classNode);
    }

    public InnerThisBytecodeExpr(ASTNode parent, ClassNode outerClass, CompilerTransformer compiler, ClassNode innerClass) {
        super(parent, outerClass);
        this.outerClass = outerClass.redirect();
        this.compiler = compiler;
        this.innerClass = innerClass;
    }

    public boolean isThis() {
        return innerClass.equals(outerClass);
    }

    protected void compile(MethodVisitor mv) {
        mv.visitVarInsn(ALOAD, 0);
        ClassNode curThis = innerClass;
        while (!curThis.equals(outerClass)) {
            compiler.context.setOuterClassInstanceUsed(curThis);
            ClassNode next = curThis.getField("this$0").getType();
            mv.visitFieldInsn(GETFIELD, BytecodeHelper.getClassInternalName(curThis), "this$0", BytecodeHelper.getTypeDescription(next));
            curThis = next;
        }
    }
}
