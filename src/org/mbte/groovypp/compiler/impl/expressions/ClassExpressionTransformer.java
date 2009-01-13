package org.mbte.groovypp.compiler.impl.expressions;

import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.impl.CompilerTransformer;
import org.mbte.groovypp.compiler.impl.BytecodeExpr;

public class ClassExpressionTransformer extends ExprTransformer<ClassExpression>{

    public Expression transform(ClassExpression exp, CompilerTransformer compiler) {

        final ClassNode type = exp.getType();
        return new BytecodeExpr(exp, ClassHelper.CLASS_Type) {
            protected void compile() {
                if (ClassHelper.isPrimitiveType(type)) {
                    mv.visitFieldInsn(GETSTATIC, BytecodeHelper.getClassInternalName(ClassHelper.getWrapper(type)), "TYPE", "Ljava/lang/Class;");
                } else {
                    if (ClassHelper.isPrimitiveType(ClassHelper.getUnwrapper(type))) {
                        mv.visitFieldInsn(GETSTATIC, BytecodeHelper.getClassInternalName(type), "TYPE", "Ljava/lang/Class;");
                    }
                    else {
                        mv.visitLdcInsn(BytecodeHelper.getClassLoadingTypeDescription(type));
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
                    }
                }
            }
        };
    }
}
