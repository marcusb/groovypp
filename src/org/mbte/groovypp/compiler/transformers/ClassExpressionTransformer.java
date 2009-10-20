package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

public class ClassExpressionTransformer extends ExprTransformer<ClassExpression> {
    public Expression transform(ClassExpression exp, CompilerTransformer compiler) {

        final ClassNode type = exp.getType();
        return new ClassExpr(exp, type);
    }

    public static BytecodeExpr newExpr(Expression exp, ClassNode type) {
        return new ClassExpr(exp, type);
    }

    private static class ClassExpr extends BytecodeExpr {
        private final ClassNode type;

        public ClassExpr(Expression exp, ClassNode type) {
            super(exp, ClassHelper.CLASS_Type);
            this.type = type;
        }

        protected void compile(MethodVisitor mv) {
            if (ClassHelper.isPrimitiveType(type)) {
                mv.visitFieldInsn(GETSTATIC, BytecodeHelper.getClassInternalName(ClassHelper.getWrapper(type)), "TYPE", "Ljava/lang/Class;");
            } else {
                mv.visitLdcInsn(BytecodeHelper.getClassLoadingTypeDescription(type));
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
            }
        }
    }
}
