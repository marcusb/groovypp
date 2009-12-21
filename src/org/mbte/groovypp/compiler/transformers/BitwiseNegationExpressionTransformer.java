package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.BitwiseNegationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import static org.codehaus.groovy.ast.ClassHelper.*;
import static org.codehaus.groovy.ast.ClassHelper.Long_TYPE;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

public class BitwiseNegationExpressionTransformer extends ExprTransformer<BitwiseNegationExpression> {
    public Expression transform(BitwiseNegationExpression exp, CompilerTransformer compiler) {
        final BytecodeExpr obj0 = (BytecodeExpr) compiler.transform(exp.getExpression());
        final ClassNode type = obj0.getType();
        final BytecodeExpr obj;
        if (type == short_TYPE
         || type == byte_TYPE
         || type == int_TYPE
         || type == long_TYPE
         || type.equals(Byte_TYPE)
         || type.equals(Short_TYPE)
         || type.equals(Integer_TYPE)
         || type.equals(Long_TYPE)) {
            final ClassNode mathType = ClassHelper.getUnwrapper(type);
            return new BytecodeExpr(exp, mathType) {
                protected void compile(MethodVisitor mv) {
                    if (mathType == ClassHelper.long_TYPE) {
                        obj0.visit(mv);
                        if (!ClassHelper.isPrimitiveType(type))
                            unbox(mathType, mv);
                        mv.visitLdcInsn(-1L);
                        mv.visitInsn(LXOR);
                    }
                    else {
                        obj0.visit(mv);
                        if (!ClassHelper.isPrimitiveType(type))
                            unbox(mathType, mv);
                        mv.visitLdcInsn(-1);
                        mv.visitInsn(IXOR);
                    }
                }
            };
        }
        else {
            if (ClassHelper.isPrimitiveType(obj0.getType()))
                obj = new BytecodeExpr(exp, TypeUtil.wrapSafely(obj0.getType())){
                    protected void compile(MethodVisitor mv) {
                        obj0.visit(mv);
                        box(obj0.getType(), mv);
                    }
                };
            else
                obj = obj0;

            MethodCallExpression mce = new MethodCallExpression(obj, "bitwiseNegate", new ArgumentListExpression());
            mce.setSourcePosition(exp);
            return compiler.transform(mce);
        }
    }
}
