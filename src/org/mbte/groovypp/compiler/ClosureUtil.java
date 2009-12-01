package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;

import java.util.*;

public class ClosureUtil {
    private static final LinkedList<MethodNode> NONE = new LinkedList<MethodNode> ();

    public static boolean likeGetter(MethodNode method) {
        return method.getName().startsWith("get")
                && ClassHelper.VOID_TYPE != method.getReturnType()
                && method.getParameters().length == 0;
    }

    public static boolean likeSetter(MethodNode method) {
        return method.getName().startsWith("set")
                && ClassHelper.VOID_TYPE == method.getReturnType()
                && method.getParameters().length == 1;
    }

    public synchronized static List<MethodNode> isOneMethodAbstract (ClassNode node) {
        if ((node.getModifiers() & Opcodes.ACC_ABSTRACT) == 0 && !node.isInterface())
            return null;

        final ClassNodeCache.ClassNodeInfo info = ClassNodeCache.getClassNodeInfo(node);
        if (info.isOneMethodAbstract == null) {
            List<MethodNode> am = node.getAbstractMethods();

            if (am == null) {
                am = (List<MethodNode>) Collections.EMPTY_LIST;
            }

            MethodNode one = null;
            for (Iterator it = am.iterator(); it.hasNext();) {
                MethodNode mn = (MethodNode) it.next();
                if (!likeGetter(mn) && !likeSetter(mn) && !traitMethod(mn)) {
                    if (one != null) {
                        info.isOneMethodAbstract = NONE;
                        return null;
                    }
                    one = mn;
                    it.remove();
                }
            }

            if (one != null)
                am.add(0, one);
            else
                if (am.size() != 1) {
                    info.isOneMethodAbstract = NONE;
                    return null;
                }

            info.isOneMethodAbstract = am;
        }

        if (info.isOneMethodAbstract == NONE)
            return null;

        return info.isOneMethodAbstract;
    }

    private static boolean traitMethod(MethodNode mn) {
        return !mn.getAnnotations(TypeUtil.HAS_DEFAULT_IMPLEMENTATION).isEmpty();
    }

    public static MethodNode isMatch(List<MethodNode> one, ClosureClassNode closureType, CompilerTransformer compiler, ClassNode baseType) {
        class Mutation {
            final Parameter p;
            final ClassNode t;

            public Mutation(ClassNode t, Parameter p) {
                this.t = t;
                this.p = p;
            }

            void mutate () {
                p.setType(t);
            }
        }

        List<Mutation> mutations = null;

        MethodNode missing = one.get(0);
        Parameter[] missingMethodParameters = missing.getParameters();
        List<MethodNode> methods = closureType.getDeclaredMethods("doCall");

        for (MethodNode method : methods) {
            
            Parameter[] closureParameters = method.getParameters();

            if (closureParameters.length != missingMethodParameters.length)
                continue;

            boolean match = true;
            for (int i = 0; i < closureParameters.length; i++) {
                Parameter closureParameter = closureParameters[i];
                Parameter missingMethodParameter = missingMethodParameters[i];

                ClassNode parameterType = missingMethodParameter.getType();
                parameterType = TypeUtil.getSubstitutedType(parameterType, baseType.redirect(), baseType);
                if (!TypeUtil.isDirectlyAssignableFrom(TypeUtil.wrapSafely(parameterType), TypeUtil.wrapSafely(closureParameter.getType()))) {
                    if (TypeUtil.isDirectlyAssignableFrom(TypeUtil.wrapSafely(closureParameter.getType()), TypeUtil.wrapSafely(parameterType))) {
                        if (mutations == null)
                            mutations = new LinkedList<Mutation> ();
                        mutations.add(new Mutation(parameterType, closureParameter));
                        continue;
                    }

                    match = false;
                    break;
                }
            }

            if (match) {
                if (mutations != null)
                    for (Mutation mutation : mutations) {
                        mutation.mutate();
                    }

                improveClosureType(closureType, baseType);
                StaticMethodBytecode.replaceMethodCode(compiler.su, method, compiler.compileStack, compiler.debug == -1 ? -1 : compiler.debug+1, compiler.policy, closureType.getName());
                return method;
            }
        }
        return null;
    }

    public static void makeOneMethodClass(final ClassNode closureType, ClassNode baseType, List<MethodNode> abstractMethods, final MethodNode doCall) {
        boolean traitMethods = false;
        int k = 0;
        for (final MethodNode missed : abstractMethods) {
            if (k == 0) {
               closureType.addMethod(
                    missed.getName(),
                    Opcodes.ACC_PUBLIC,
                    getSubstitutedReturnType(doCall, missed, closureType, baseType),
                    missed.getParameters(),
                    ClassNode.EMPTY_ARRAY,
                    new BytecodeSequence(
                            new BytecodeInstruction() {
                                public void visit(MethodVisitor mv) {
                                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                                    Parameter pp[] = missed.getParameters();
                                    for (int i = 0, k = 1; i != pp.length; ++i) {
                                        final ClassNode type = pp[i].getType();
                                        ClassNode expectedType = doCall.getParameters()[i].getType();
                                        if (ClassHelper.isPrimitiveType(type)) {
                                            if (type == ClassHelper.long_TYPE) {
                                                mv.visitVarInsn(Opcodes.LLOAD, k++);
                                                k++;
                                            } else if (type == ClassHelper.double_TYPE) {
                                                mv.visitVarInsn(Opcodes.DLOAD, k++);
                                                k++;
                                            } else if (type == ClassHelper.float_TYPE) {
                                                mv.visitVarInsn(Opcodes.FLOAD, k++);
                                            } else {
                                                mv.visitVarInsn(Opcodes.ILOAD, k++);
                                            }
                                            BytecodeExpr.box(type, mv);
                                            BytecodeExpr.cast(TypeUtil.wrapSafely(type), TypeUtil.wrapSafely(expectedType), mv);
                                        } else {
                                            mv.visitVarInsn(Opcodes.ALOAD, k++);
                                            BytecodeExpr.checkCast(TypeUtil.wrapSafely(expectedType), mv);
                                        }
                                        BytecodeExpr.unbox(expectedType, mv);
                                    }
                                    mv.visitMethodInsn(
                                            Opcodes.INVOKEVIRTUAL,
                                            BytecodeHelper.getClassInternalName(doCall.getDeclaringClass()),
                                            doCall.getName(),
                                            BytecodeHelper.getMethodDescriptor(doCall.getReturnType(), doCall.getParameters())
                                    );

                                    if (missed.getReturnType() != ClassHelper.VOID_TYPE) {
                                        BytecodeExpr.box(doCall.getReturnType(), mv);
                                        BytecodeExpr.checkCast(TypeUtil.wrapSafely(doCall.getReturnType()), mv);
                                        BytecodeExpr.unbox(missed.getReturnType(), mv);
                                    }
                                    BytecodeExpr.doReturn(mv, missed.getReturnType());
                                }
                            }
                    ));
            }
            else {
                if (ClosureUtil.likeGetter(missed)) {
                    String pname = missed.getName().substring(3);
                    pname = Character.toLowerCase(pname.charAt(0)) + pname.substring(1);
                    closureType.addProperty(pname, Opcodes.ACC_PUBLIC, missed.getReturnType(), null, null, null);
                }
                else {
                    if (ClosureUtil.likeSetter(missed)) {
                        String pname = missed.getName().substring(3);
                        pname = Character.toLowerCase(pname.charAt(0)) + pname.substring(1);
                        closureType.addProperty(pname, Opcodes.ACC_PUBLIC, missed.getParameters()[0].getType(), null, null, null);
                    }
                    else {
                        if (ClosureUtil.traitMethod(missed)) {
                            traitMethods = true;
                        }
                    }
                }
            }
            k++;
        }

        if (traitMethods)
            TraitASTTransformFinal.improveAbstractMethods(closureType);
    }

    private static ClassNode getSubstitutedReturnType(MethodNode doCall, MethodNode missed, ClassNode closureType,
                                                      ClassNode baseType) {
        ClassNode returnType = missed.getReturnType();
        if (missed.getParameters().length == doCall.getParameters().length) {
            int nParams = missed.getParameters().length;
            ClassNode declaringClass = missed.getDeclaringClass();
            GenericsType[] typeVars = declaringClass.getGenericsTypes();
            if (typeVars != null && typeVars.length > 0) {
                ClassNode[] formals = new ClassNode[nParams + 1];
                ClassNode[] actuals = new ClassNode[nParams + 1];
                for (int i = 0; i < nParams; i++) {
                    actuals[i] = doCall.getParameters()[i].getType();
                    formals[i] = missed.getParameters()[i].getType();
                }
                actuals[actuals.length - 1] = doCall.getReturnType();
                formals[formals.length - 1] = missed.getReturnType();
                ClassNode[] unified = TypeUnification.inferTypeArguments(typeVars, formals, actuals);
                if (TypeUnification.totalInference(unified)) {
                    GenericsType[] genericTypes = new GenericsType[unified.length];
                    for (int i = 0; i < genericTypes.length; i++) {
                        genericTypes[i] = new GenericsType(unified[i]);
                    }
                    ClassNode newBase = TypeUtil.withGenericTypes(baseType, genericTypes);
                    improveClosureType(closureType, newBase);
                    returnType = TypeUtil.getSubstitutedType(returnType, declaringClass, newBase);
                }
            }
        }
        return returnType;
    }

    public static void improveClosureType(final ClassNode closureType, ClassNode baseType) {
        if (baseType.isInterface()) {
            closureType.setInterfaces(new ClassNode[]{baseType});
            closureType.setSuperClass(ClassHelper.OBJECT_TYPE);
        } else {
            closureType.setInterfaces(ClassNode.EMPTY_ARRAY);
            closureType.setSuperClass(baseType);
        }
    }

    public static ClosureMethodNode createDependentMethod(final ClassNode newType, final ClosureMethodNode _doCallMethod) {
        ClosureMethodNode _doCallMethodDef = new ClosureMethodNode.Dependent(
                _doCallMethod,
                _doCallMethod.getName(),
                Opcodes.ACC_PUBLIC,
                ClassHelper.OBJECT_TYPE,
                Parameter.EMPTY_ARRAY,
                new BytecodeSequence(new BytecodeInstruction(){
                    public void visit(MethodVisitor mv) {
                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                        final ClassNode type = _doCallMethod.getParameters()[0].getType();
                        if (ClassHelper.isPrimitiveType(type)) {
                            if (type == ClassHelper.long_TYPE) {
                                mv.visitInsn(Opcodes.LCONST_0);
                            } else if (type == ClassHelper.double_TYPE) {
                                mv.visitInsn(Opcodes.DCONST_0);
                            } else if (type == ClassHelper.float_TYPE) {
                                mv.visitInsn(Opcodes.FCONST_0);
                            } else {
                                mv.visitInsn(Opcodes.ICONST_0);
                            }
                        } else {
                            mv.visitInsn(Opcodes.ACONST_NULL);
                        }
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, BytecodeHelper.getClassInternalName(newType), _doCallMethod.getName(), BytecodeHelper.getMethodDescriptor(_doCallMethod.getReturnType(),_doCallMethod.getParameters()));
                        BytecodeExpr.doReturn (mv, _doCallMethod.getReturnType());
                    }
                })){

            @Override
            public ClassNode getReturnType() {
                return _doCallMethod.getReturnType();
            }
        };

        newType.addMethod(_doCallMethodDef);
        return _doCallMethodDef;
    }

    public static void createClosureConstructor(final ClassNode newType, final Parameter[] constrParams, Expression superArgs) {

        final ClassNode superClass = newType.getSuperClass();

        final Parameter[] finalConstrParams;
        final ArgumentListExpression superCallArgs = new ArgumentListExpression();
        if (superArgs != null) {
            final ArgumentListExpression args = (ArgumentListExpression) superArgs;
            if (args.getExpressions().size() > 0) {
                Parameter [] newParams = new Parameter [constrParams.length + args.getExpressions().size()];
                System.arraycopy(constrParams, 0, newParams, 0, constrParams.length);
                for (int i = 0; i != args.getExpressions().size(); ++i) {
                    final Parameter parameter = new Parameter(args.getExpressions().get(i).getType(), "$super$param$" + i);
                    newParams [i+constrParams.length] = parameter;
                    superCallArgs.addExpression(new VariableExpression(parameter));
                }
                finalConstrParams = newParams;
            }
            else
                finalConstrParams = constrParams;
        }
        else {
            if (superClass == ClassHelper.CLOSURE_TYPE) {
                superCallArgs.addExpression(new VariableExpression(constrParams[0]));
            }
            finalConstrParams = constrParams;
        }

        ConstructorCallExpression superCall = new ConstructorCallExpression(ClassNode.SUPER, superCallArgs);

        BytecodeSequence fieldInit = new BytecodeSequence(new BytecodeInstruction() {
            public void visit(MethodVisitor mv) {
                for (int i = 0, k = 1; i != constrParams.length; i++) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);

                    final ClassNode type = constrParams[i].getType();
                    if (ClassHelper.isPrimitiveType(type)) {
                        if (type == ClassHelper.long_TYPE) {
                            mv.visitVarInsn(Opcodes.LLOAD, k++);
                            k++;
                        } else if (type == ClassHelper.double_TYPE) {
                            mv.visitVarInsn(Opcodes.DLOAD, k++);
                            k++;
                        } else if (type == ClassHelper.float_TYPE) {
                            mv.visitVarInsn(Opcodes.FLOAD, k++);
                        } else {
                            mv.visitVarInsn(Opcodes.ILOAD, k++);
                        }
                    } else {
                        mv.visitVarInsn(Opcodes.ALOAD, k++);
                    }
                    mv.visitFieldInsn(Opcodes.PUTFIELD, BytecodeHelper.getClassInternalName(newType), constrParams[i].getName(), BytecodeHelper.getTypeDescription(type));
                }
                mv.visitInsn(Opcodes.RETURN);
            }
        });


        ConstructorNode cn = new ConstructorNode(
                    Opcodes.ACC_PUBLIC,
                    finalConstrParams,
                    ClassNode.EMPTY_ARRAY,
                new BlockStatement(new Statement[] { new ExpressionStatement(superCall), fieldInit, ReturnStatement.RETURN_NULL_OR_VOID}, new VariableScope()));
        newType.addConstructor(cn);

        if (newType.getOuterClass() != null && newType.getMethods("methodMissing").isEmpty()) {
            newType.addMethod("methodMissing",
                    Opcodes.ACC_PUBLIC,
                    ClassHelper.OBJECT_TYPE,
                    new Parameter[] {
                            new Parameter(ClassHelper.STRING_TYPE, "name"),
                            new Parameter(ClassHelper.OBJECT_TYPE, "args")},
                    ClassNode.EMPTY_ARRAY,
                    new BytecodeSequence(new BytecodeInstruction(){
                        public void visit(MethodVisitor mv) {
                                mv.visitVarInsn(Opcodes.ALOAD, 0);
                                mv.visitFieldInsn(Opcodes.GETFIELD, BytecodeHelper.getClassInternalName(newType), "this$0", BytecodeHelper.getTypeDescription(newType.getOuterClass()));
                                mv.visitVarInsn(Opcodes.ALOAD, 1);
                                mv.visitVarInsn(Opcodes.ALOAD, 2);
                                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/codehaus/groovy/runtime/InvokerHelper", "invokeMethod", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
                                mv.visitInsn(Opcodes.ARETURN);
                        }
                    }));
        }
    }

    public static void addFields(ClosureExpression ce, ClassNode newType, CompilerTransformer compiler) {
        for(Iterator it = ce.getVariableScope().getReferencedLocalVariablesIterator(); it.hasNext(); ) {
            Variable astVar = (Variable) it.next();
            final org.codehaus.groovy.classgen.Variable var = compiler.compileStack.getVariable(astVar.getName(), false);

            ClassNode vtype;
            if (var != null) {
                vtype = compiler.getLocalVarInferenceTypes().get(astVar);
                if (vtype == null)
                   vtype = var.getType();
            }
            else {
                vtype = compiler.methodNode.getDeclaringClass().getField(astVar.getName()).getType();
            }

            if (newType.getDeclaredField(astVar.getName()) == null) {
                newType.addField(astVar.getName(), Opcodes.ACC_FINAL, vtype, null);
            }
        }
    }

    public static Parameter[] createClosureConstructorParams(ClassNode newType) {
        List<FieldNode> fields = newType.getFields();

        final Parameter constrParams [] = new Parameter[fields.size()];

        int k = 0;
        FieldNode ownerField = newType.getField("this$0");
        if (ownerField != null)
            constrParams [k++] = new Parameter(ownerField.getType(), "this$0");
        for (int i = 0; i != fields.size(); ++i) {
            final FieldNode fieldNode = fields.get(i);
            if (!fieldNode.getName().equals("this$0"))
                constrParams [k++] = new Parameter(fieldNode.getType(), fieldNode.getName());
        }
        return constrParams;
    }

    public static void instantiateClass(ClassNode type, CompilerTransformer compiler, Parameter[] constrParams, Expression superArgs, MethodVisitor mv) {
        type.getModule().addClass(type);

        final String classInternalName = BytecodeHelper.getClassInternalName(type);
        mv.visitTypeInsn(Opcodes.NEW, classInternalName);
        mv.visitInsn(Opcodes.DUP);

        final ConstructorNode constructorNode = type.getDeclaredConstructors().get(0);

        for (int i = 0; i != constrParams.length; i++) {
            final String name = constrParams[i].getName();

            if ("this$0".equals(name)) {
                mv.visitVarInsn(Opcodes.ALOAD,0);
            }
            else {
                final org.codehaus.groovy.classgen.Variable var = compiler.compileStack.getVariable(name, false);
                if (var != null) {
                    BytecodeExpr.loadVar(var, mv);
                    BytecodeExpr.unbox(var.getType(), mv);
                    if (!constrParams[i].getType().equals(var.getType())) {
                        BytecodeExpr.checkCast(constrParams[i].getType(), mv);
                    }
                }
                else {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitFieldInsn(Opcodes.GETFIELD, BytecodeHelper.getClassInternalName(compiler.methodNode.getDeclaringClass()), name, BytecodeHelper.getTypeDescription(constrParams[i].getType()));
                }
            }
        }

        if (superArgs != null) {
            final List<Expression> list = ((ArgumentListExpression) superArgs).getExpressions();
            for (int i = 0; i != list.size(); ++i)
                ((BytecodeExpr)list.get(i)).visit(mv);
        }

        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, classInternalName, "<init>", BytecodeHelper.getMethodDescriptor(ClassHelper.VOID_TYPE, constructorNode.getParameters()));
    }
}
