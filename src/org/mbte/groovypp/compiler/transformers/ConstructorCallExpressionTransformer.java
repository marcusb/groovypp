package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.transformers.ExprTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;

public class ConstructorCallExpressionTransformer extends ExprTransformer<ConstructorCallExpression> {

    public Expression transform(ConstructorCallExpression exp, CompilerTransformer compiler) {
        final Expression newArgs = compiler.transform(exp.getArguments());
        final ClassNode[] argTypes = compiler.exprToTypeArray(newArgs);

        final MethodNode constructor = compiler.findConstructor(exp.getType(), argTypes);

        if (constructor != null) {
            return new BytecodeExpr(exp, exp.getType()) {
                protected void compile() {
                    final String classInternalName = BytecodeHelper.getClassInternalName(getType());
                    mv.visitTypeInsn(NEW, classInternalName);
                    mv.visitInsn(DUP);

                    ArgumentListExpression bargs = (ArgumentListExpression) newArgs;
                    for (int i = 0; i != bargs.getExpressions().size(); ++i) {
                        BytecodeExpr be = (BytecodeExpr) bargs.getExpressions().get(i);
                        be.visit(mv);
                        final ClassNode paramType = constructor.getParameters()[i].getType();
                        final ClassNode type = be.getType();
                        box(type);
                        be.cast(ClassHelper.getWrapper(type), ClassHelper.getWrapper(paramType));
                        be.unbox(paramType);
                    }

                    mv.visitMethodInsn(INVOKESPECIAL, classInternalName, "<init>", BytecodeHelper.getMethodDescriptor(ClassHelper.VOID_TYPE, constructor.getParameters()));
                }
            };
        }
        else {
            compiler.addError("Can't find constructor", exp);
            return null;
        }
    }
}
