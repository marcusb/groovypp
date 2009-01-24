package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.transformers.ExprTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ConstructorCallExpressionTransformer extends ExprTransformer<ConstructorCallExpression> {

    public Expression transform(ConstructorCallExpression exp, CompilerTransformer compiler) {
        final Expression newArgs = compiler.transform(exp.getArguments());
        final ClassNode[] argTypes = compiler.exprToTypeArray(newArgs);

        MethodNode constructor = compiler.findConstructor(exp.getType(), argTypes);
        if (constructor == null) {
            if (argTypes.length > 0 && argTypes[argTypes.length-1] != null && argTypes[argTypes.length-1].implementsInterface(TypeUtil.TCLOSURE)) {
                final ClassNode oarg = argTypes[argTypes.length-1];
                argTypes[argTypes.length-1] = null;
                constructor = compiler.findConstructor(exp.getType(), argTypes);
                if (constructor != null) {
                    Parameter p [] = constructor.getParameters();
                    if (p.length == argTypes.length) {
                        ClassNode argType = p[p.length - 1].getType();
                        if (argType.isInterface() || (argType.getModifiers() & ACC_ABSTRACT) != 0) {
                            final List am = argType.getAbstractMethods();

                            ArrayList<MethodNode> props = null;
                            for (Iterator it = am.iterator(); it.hasNext(); ) {
                                MethodNode mn = (MethodNode) it.next();
                                if (MethodCallExpressionTransformer.likeGetter(mn) || MethodCallExpressionTransformer.likeSetter(mn)) {
                                    it.remove();
                                    if (props == null)
                                        props = new ArrayList<MethodNode>();
                                    props.add(mn);
                                }
                            }

                            if (am.size() <= 1) {
                                MethodCallExpressionTransformer.makeOneMethodClass(oarg, argType, am);

                                if (props != null) {
                                   for (MethodNode mn : props) {
                                       if (MethodCallExpressionTransformer.likeGetter(mn)) {
                                           String pname = mn.getName().substring(3);
                                           pname = Character.toLowerCase(pname.charAt(0)) + pname.substring(1);
                                           oarg.addProperty(pname, ACC_PUBLIC, mn.getReturnType(), null, null, null);
                                       }

                                       if (MethodCallExpressionTransformer.likeSetter(mn)) {
                                           String pname = mn.getName().substring(3);
                                           pname = Character.toLowerCase(pname.charAt(0)) + pname.substring(1);
                                           oarg.addProperty(pname, ACC_PUBLIC, mn.getParameters()[0].getType(), null, null, null);
                                       }
                                   }
                                }
                            }
                        }
                    }
                }
                argTypes[argTypes.length-1] = oarg;
            }
        }

        if (constructor != null) {
            final MethodNode constructor1 = constructor;
            return new BytecodeExpr(exp, exp.getType()) {
                protected void compile() {
                    final String classInternalName = BytecodeHelper.getClassInternalName(getType());
                    mv.visitTypeInsn(NEW, classInternalName);
                    mv.visitInsn(DUP);

                    ArgumentListExpression bargs = (ArgumentListExpression) newArgs;
                    for (int i = 0; i != bargs.getExpressions().size(); ++i) {
                        BytecodeExpr be = (BytecodeExpr) bargs.getExpressions().get(i);
                        be.visit(mv);
                        final ClassNode paramType = constructor1.getParameters()[i].getType();
                        final ClassNode type = be.getType();
                        box(type);
                        be.cast(ClassHelper.getWrapper(type), ClassHelper.getWrapper(paramType));
                        be.unbox(paramType);
                    }

                    mv.visitMethodInsn(INVOKESPECIAL, classInternalName, "<init>", BytecodeHelper.getMethodDescriptor(ClassHelper.VOID_TYPE, constructor1.getParameters()));
                }
            };
        }

        if (exp.getArguments() instanceof TupleExpression && ((TupleExpression)exp.getArguments()).getExpressions().size() == 1 && ((TupleExpression)exp.getArguments()).getExpressions().get(0) instanceof MapExpression) {
            MapExpression me = (MapExpression) ((TupleExpression)exp.getArguments()).getExpressions().get(0);

            constructor = compiler.findConstructor(exp.getType(), ClassNode.EMPTY_ARRAY);
            final ArrayList<BytecodeExpr> propSetters = new ArrayList<BytecodeExpr> ();

            for (Iterator it = me.getMapEntryExpressions().iterator(); it.hasNext(); ) {
                MapEntryExpression mee = (MapEntryExpression) it.next();

                BytecodeExpr obj = new BytecodeExpr(mee, exp.getType()) {
                    protected void compile() {
                        mv.visitInsn(DUP);
                    }
                };

                propSetters.add(
                        (BytecodeExpr) compiler.transform(
                        new BinaryExpression(
                                new PropertyExpression(
                                    obj,
                                    mee.getKeyExpression()
                                ),
                                Token.newSymbol(Types.ASSIGN, -1, -1),
                                mee.getValueExpression()
                           )
                        )
                );
            }

            if (constructor != null) {
                return new BytecodeExpr(exp, exp.getType()) {
                    protected void compile() {
                        final String classInternalName = BytecodeHelper.getClassInternalName(getType());
                        mv.visitTypeInsn(NEW, classInternalName);
                        mv.visitInsn(DUP);
                        mv.visitMethodInsn(INVOKESPECIAL, classInternalName, "<init>", "()V");

                        for (BytecodeExpr prop : propSetters) {
                            prop.visit(mv);
                            pop(prop.getType());
                        }
                    }
                };
            }
        }

        compiler.addError("Can't find constructor", exp);
        return null;
    }

}
