package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.BytecodeSpreadExpr;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

public class ListExpressionTransformer extends ExprTransformer<ListExpression> {
    public Expression transform(final ListExpression exp, final CompilerTransformer compiler) {
        ClassNode newType = new ClassNode(compiler.getNextClosureName(), ACC_PUBLIC, ClassHelper.OBJECT_TYPE);
        newType.setInterfaces(new ClassNode[] {TypeUtil.TLIST});
        return new UntransformedListExpr(exp, newType);
    }

    public static class UntransformedListExpr extends BytecodeExpr {
        public final ListExpression exp;

        public UntransformedListExpr(ListExpression exp, ClassNode type) {
            super(exp, type);
            this.exp = exp;
        }

        protected void compile(MethodVisitor mv) {
            throw new UnsupportedOperationException();
        }
    }

    public static class TransformedListExpr extends BytecodeExpr {
        public final ListExpression exp;
        private CompilerTransformer compiler;

        public TransformedListExpr(ListExpression exp, ClassNode collType, CompilerTransformer compiler) {
            super(exp, collType);
            this.exp = exp;
            this.compiler = compiler;

            final List<Expression> list = exp.getExpressions();
            ClassNode genericArg = null;
            for (int i = 0; i != list.size(); ++i) {
                Expression transformed = compiler.transform(list.get(i));
                if (transformed instanceof UntransformedListExpr)
                    transformed = new TransformedListExpr(((UntransformedListExpr)transformed).exp, TypeUtil.ARRAY_LIST_TYPE, compiler);
                if (transformed instanceof MapExpressionTransformer.UntransformedMapExpr)
                    transformed = new MapExpressionTransformer.TransformedMapExpr(((MapExpressionTransformer.UntransformedMapExpr)transformed).exp, compiler);
                list.set(i, transformed);

                if (!(transformed instanceof BytecodeSpreadExpr))
                    genericArg = genericArg == null ? transformed.getType() :
                            TypeUtil.commonType(genericArg, transformed.getType());
            }

            if (genericArg != null) {
                genericArg = TypeUtil.wrapSafely(genericArg);
                setType( TypeUtil.withGenericTypes(collType, new GenericsType[] {new GenericsType(genericArg)}));
            }
        }

        protected void compile(MethodVisitor mv) {
            final List list = exp.getExpressions();
            String classInternalName = BytecodeHelper.getClassInternalName(getType());
            mv.visitTypeInsn(NEW, classInternalName);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL,classInternalName,"<init>","()V");
            for (int i = 0; i != list.size(); ++i) {
                final BytecodeExpr be = (BytecodeExpr) list.get(i);
                mv.visitInsn(DUP);
                be.visit(mv);
                if (be instanceof BytecodeSpreadExpr)
                    mv.visitMethodInsn(INVOKEINTERFACE,"java/util/Collection","addAll","(Ljava/util/Collection;)Z");
                else {
                    box(be.getType(), mv);
                    mv.visitMethodInsn(INVOKEINTERFACE,"java/util/Collection","add","(Ljava/lang/Object;)Z");
                }
                mv.visitInsn(POP);
            }
        }
    }
}
