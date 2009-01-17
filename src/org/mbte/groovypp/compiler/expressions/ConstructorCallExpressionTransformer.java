package org.mbte.groovypp.compiler.expressions;

import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;

import java.util.List;
import java.util.ArrayList;

public class ConstructorCallExpressionTransformer extends ExprTransformer<ConstructorCallExpression>{
    private static final ClassNode[] ARGS = new ClassNode[] {TypeUtil.LINKED_HASH_MAP_TYPE};

    public Expression transform(final ConstructorCallExpression exp, CompilerTransformer compiler) {
        List argList = ((TupleExpression) exp.getArguments()).getExpressions();
        if (argList.size() == 1 && argList.get(0) instanceof MapExpression) {
            MethodNode constructor = compiler.findConstructor(exp.getType(), ARGS);
            if (constructor != null) {
                final Expression newArgs = compiler.transform(exp.getArguments());
                return new MyBytecodeExpr(exp, newArgs, constructor);
            }
            else {
                constructor = compiler.findConstructor(exp.getType(), ClassNode.EMPTY_ARRAY);
                if (constructor != null) {

                    MapExpression me = (MapExpression) ((TupleExpression)exp.getArguments()).getExpressions().get(0);
                    final List<BytecodeExpr> props = new ArrayList<BytecodeExpr>(me.getMapEntryExpressions().size());
                    for (Object o : me.getMapEntryExpressions()) {
                        MapEntryExpression mee = (MapEntryExpression) o;
                        BytecodeExpr onStack = new BytecodeExpr(mee, exp.getType()) {
                            protected void compile() {
                            }
                        };
                        PropertyExpression prop = new PropertyExpression(onStack, mee.getKeyExpression().getText());
                        prop.setSourcePosition(mee);
                        props.add((BytecodeExpr) compiler.transform(
                                new BinaryExpression(
                                        prop,
                                        Token.newSymbol(Types.ASSIGN, -1, -1),
                                        mee.getValueExpression()
                                )));
                    }

                    return new BytecodeExpr(exp, exp.getType()) {
                        protected void compile() {
                            final String classInternalName = BytecodeHelper.getClassInternalName(getType());
                            mv.visitTypeInsn(NEW, classInternalName);
                            mv.visitInsn(DUP);
                            mv.visitMethodInsn(INVOKESPECIAL, classInternalName, "<init>", "()V");

                            for (BytecodeExpr prop : props) {
                                mv.visitInsn(DUP);
                                prop.visit(mv);
                                pop(prop.getType());
                            }
                        }
                    };
                }
            }
        }
        else {
            final Expression newArgs = compiler.transform(exp.getArguments());
            final ClassNode[] argTypes = compiler.exprToTypeArray(newArgs);

            final MethodNode constructor = compiler.findConstructor(exp.getType(), argTypes);

            if (constructor != null) {
                return new MyBytecodeExpr(exp, newArgs, constructor);
            }
        }

        compiler.addError("Can't find constructor", exp);
        return null;
    }

    private static class MyBytecodeExpr extends BytecodeExpr {
        private final Expression newArgs;
        private final MethodNode constructor;

        public MyBytecodeExpr(ConstructorCallExpression exp, Expression newArgs, MethodNode constructor) {
            super(exp, exp.getType());
            this.newArgs = newArgs;
            this.constructor = constructor;
        }

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
    }
}
