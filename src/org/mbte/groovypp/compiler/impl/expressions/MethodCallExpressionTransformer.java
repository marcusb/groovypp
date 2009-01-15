package org.mbte.groovypp.compiler.impl.expressions;

import groovy.lang.CompilePolicy;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.mbte.groovypp.compiler.impl.CompiledMethodBytecodeExpr;
import org.mbte.groovypp.compiler.impl.CompilerTransformer;
import org.mbte.groovypp.compiler.impl.TypeUtil;
import org.mbte.groovypp.compiler.impl.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

public class MethodCallExpressionTransformer extends ExprTransformer<MethodCallExpression>{
    public Expression transform(final MethodCallExpression exp, final CompilerTransformer compiler) {
        Expression args = compiler.transform(exp.getArguments());
        exp.setArguments(args);

        if (exp.isSpreadSafe()) {
            compiler.addError("Spread operator is not supported by static compiler", exp);
            return null;
        }

        Object method = exp.getMethod();
        String methodName = null;
        if (!(method instanceof ConstantExpression) || !(((ConstantExpression) method).getValue() instanceof String)) {
          if (compiler.policy == CompilePolicy.STATIC) {
              compiler.addError("Non-static method name", exp);
              return null;
          }
          else {
              return createDynamicCall(exp, compiler);
          }
        }
        else {
          methodName = (String) ((ConstantExpression) method).getValue();
        }

        BytecodeExpr object;
        ClassNode type;
        MethodNode foundMethod = null;
        if (exp.getObjectExpression() instanceof ClassExpression) {
            object = null;
            type = exp.getObjectExpression().getType();
        }
        else {
            if (!exp.isImplicitThis() || !compiler.methodNode.getName().equals("$doCall")) {
                object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
                type = object.getType();
            } else {
                type = compiler.methodNode.getParameters()[0].getType();
                object = new BytecodeExpr(exp,type){
                    protected void compile() {
                        mv.visitVarInsn(ALOAD, 0);
                    }
                };
            }
        }

//        TODO:
//        if (type.implementsInterface(ClassHelper.make(GroovyInterceptable.class))) {
//
//        }

        final ClassNode[] argTypes = compiler.exprToTypeArray(args);
        type = ClassHelper.getWrapper(type);
        foundMethod = compiler.findMethod(type, methodName, argTypes);
        if (foundMethod == null) {
            if (compiler.methodNode.getName().equals("$doCall")) {
                // we are in closure code
                foundMethod = compiler.findMethod(compiler.classNode, methodName, argTypes);
                if (foundMethod != null) {
                    object = new BytecodeExpr(exp, compiler.classNode) {
                        protected void compile() {
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitMethodInsn(INVOKEVIRTUAL, "groovy/lang/Closure", "getOwner", "()Ljava/lang/Object;");
                            mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(compiler.classNode));
                        }
                    };
                }
                else {
                    // not found in owner
                    // let's try delegate
//                    foundMethod = findMethod(delegateType, methodName, args);
                    if (type.implementsInterface(TypeUtil.TCLOSURE)) {
                        final ClassNode tclosure = type.getInterfaces()[0];
                        final GenericsType[] genericsTypes = tclosure.getGenericsTypes();
                        if (genericsTypes != null) {
                            final ClassNode delegateType = genericsTypes[0].getType();
                            foundMethod = compiler.findMethod(delegateType, methodName, argTypes);
                            if (foundMethod != null) {
                                object = new BytecodeExpr(exp, compiler.classNode) {
                                    protected void compile() {
                                        mv.visitVarInsn(ALOAD, 0);
                                        mv.visitMethodInsn(INVOKEVIRTUAL, "groovy/lang/Closure", "getDelegate", "()Ljava/lang/Object;");
                                        mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(delegateType));
                                    }
                                };
                            }
                        }
                    }
                }
            }
        }

        if (foundMethod == null) {
            if (argTypes.length > 0 && argTypes[argTypes.length-1].implementsInterface(TypeUtil.TCLOSURE)) {
                final ClassNode oarg = argTypes[argTypes.length-1];
                argTypes[argTypes.length-1] = null;
                foundMethod = compiler.findMethod(type, methodName, argTypes);
                if (foundMethod != null) {
                    Parameter p [] = foundMethod.getParameters();
                    if (p.length == argTypes.length) {
                        if (p[p.length-1].getType().isInterface()) {
                            final ClassNode tp = p[p.length - 1].getType();
                            final List am = tp.getAbstractMethods();
                            if (am.size() <= 1) {
                                final ClassNode[] oifaces = oarg.getInterfaces();
                                ClassNode [] ifaces = new ClassNode[oifaces.length+1];
                                System.arraycopy(oifaces, 0, ifaces, 1, oifaces.length);
                                ifaces [0] = tp;
                                oarg.setInterfaces(ifaces);

                                if (am.size() == 1) {
                                    final MethodNode missed = (MethodNode) am.get(0);
                                    oarg.addMethod(
                                            missed.getName(),
                                            ACC_PUBLIC,
                                            missed.getReturnType(),
                                            missed.getParameters(),
                                            ClassNode.EMPTY_ARRAY,
                                            new BytecodeSequence(
                                                    new BytecodeInstruction() {
                                                        public void visit(MethodVisitor mv) {
                                                            mv.visitVarInsn(ALOAD, 0);
                                                            Parameter pp []  = missed.getParameters();
                                                            for (int i = 0, k = 1; i != pp.length; ++i) {
                                                                final ClassNode type = pp[i].getType();
                                                                if (ClassHelper.isPrimitiveType(type)) {
                                                                    if (type == ClassHelper.long_TYPE) {
                                                                        mv.visitVarInsn(LLOAD, k++);
                                                                        k++;
                                                                    }
                                                                    else if (type == ClassHelper.double_TYPE) {
                                                                        mv.visitVarInsn(DLOAD, k++);
                                                                        k++;
                                                                    }
                                                                    else if (type == ClassHelper.float_TYPE) {
                                                                        mv.visitVarInsn(FLOAD, k++);
                                                                    }
                                                                    else {
                                                                        mv.visitVarInsn(ILOAD, k++);
                                                                    }
                                                                }
                                                                else {
                                                                    mv.visitVarInsn(ALOAD, k++);
                                                                }
                                                            }
                                                            mv.visitMethodInsn(
                                                                    INVOKEVIRTUAL,
                                                                    BytecodeHelper.getClassInternalName(oarg),
                                                                    "doCall",
                                                                    BytecodeHelper.getMethodDescriptor(ClassHelper.OBJECT_TYPE,pp)
                                                            );

                                                            if (ClassHelper.isPrimitiveType(missed.getReturnType())) {
                                                                String returnString = "(Ljava/lang/Object;)" + BytecodeHelper.getTypeDescription(missed.getReturnType());
                                                                mv.visitMethodInsn(
                                                                        Opcodes.INVOKESTATIC,
                                                                        BytecodeHelper.getClassInternalName(DefaultTypeTransformation.class.getName()),
                                                                        missed.getReturnType().getName() + "Unbox",
                                                                        returnString);
                                                            }
                                                            BytecodeExpr.doReturn(mv, missed.getReturnType());
                                                        }
                                                    }
                                            ));
                                }
                            }
                        }
                    }
                }
                argTypes[argTypes.length-1] = oarg;
            }
        }

        if (foundMethod == null) {
            if (compiler.policy == CompilePolicy.STATIC) {
                compiler.addError("Can't find or choose method '" + methodName + "' for type " + type.getName(), exp);
                return null;
            }
            else {
                return createDynamicCall(exp, compiler);
            }
        }

        if (object == null && !foundMethod.isStatic()) {
            if (compiler.policy == CompilePolicy.STATIC) {
                compiler.addError("Can't call non-static method '" + methodName + "' for type " + type.getName(), exp);
                return null;
            }
            else {
                return createDynamicCall(exp, compiler);
            }
        }

        final boolean safe = exp.isSafe();
        final BytecodeExpr result = new CompiledMethodBytecodeExpr(exp, foundMethod, object, (ArgumentListExpression) args);

        result.setSourcePosition(exp);
        return result;
    }

    private Expression createDynamicCall(final MethodCallExpression exp, CompilerTransformer compiler) {
        final BytecodeExpr methodExpr = (BytecodeExpr) compiler.transform(exp.getMethod());
        final BytecodeExpr object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
        return new BytecodeExpr(exp, ClassHelper.OBJECT_TYPE) {
            protected void compile() {
                mv.visitInsn(ACONST_NULL);
                object.visit(mv);
                box(object.getType());

                methodExpr.visit(mv);

                final List args = ((ArgumentListExpression) exp.getArguments()).getExpressions();
                mv.visitLdcInsn(args.size());
                mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
                for (int j = 0; j != args.size(); ++j) {
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn(j);
                    ((BytecodeExpr)args.get(j)).visit(mv);
                    mv.visitInsn(AASTORE);
                }
                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/ScriptBytecodeAdapter", "invokeMethodN", "(Ljava/lang/Class;Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
            }
        };
    }
}
