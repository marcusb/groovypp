package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.util.FastArray;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.*;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;

/**
 * Cast processing rules:
 * a) as operator always go via asType method
 * b) both numerical types always goes via primitive types
 * c) if we cast staticly to lower type we keep upper (except if upper NullType)
 * d) otherwise we do direct cast
 * e) primitive types goes via boxing and Number
 */
public class CastExpressionTransformer extends ExprTransformer<CastExpression> {
    public BytecodeExpr transform(final CastExpression exp, CompilerTransformer compiler) {

        if (exp.getExpression() instanceof ListExpression) {
            ListExpression listExpression = (ListExpression) exp.getExpression();

            if (exp.getType().isArray()) {
                ClassNode componentType = exp.getType().getComponentType();
                improveListTypes(listExpression, componentType);
                return listToArray(exp, compiler);
            }

            if(exp.getType().implementsInterface(TypeUtil.COLLECTION_TYPE)) {
                ClassNode componentType = compiler.getCollectionType(exp.getType());
                improveListTypes(listExpression, componentType);
                final List list = listExpression.getExpressions();
                for (int i = 0; i != list.size(); ++i) {
                    list.set(i, compiler.transform((Expression) list.get(i)));
                }

                ClassNode collType = calcResultCollectionType(exp, componentType, compiler);
                return new ListExpressionTransformer.ResolvedListExpression(listExpression, collType);
            }
        }

        if (exp.getExpression() instanceof MapExpression) {
            MapExpression mapExpression = (MapExpression) exp.getExpression();

            if (!exp.getType().implementsInterface(ClassHelper.MAP_TYPE)
             && !exp.getType().equals(ClassHelper.MAP_TYPE)
             && !TypeUtil.isAssignableFrom(exp.getType(), TypeUtil.LINKED_HASH_MAP_TYPE)) {
                return buildClassFromMap (mapExpression, exp.getType(), compiler);
            }
        }

        final BytecodeExpr expr = (BytecodeExpr) compiler.transform(exp.getExpression());

        if (expr.getType().implementsInterface(TypeUtil.TCLOSURE)) {
            List<MethodNode> one = ClosureUtil.isOneMethodAbstract(exp.getType());
            MethodNode doCall = one == null ? null : ClosureUtil.isMatch(one, (ClosureClassNode) expr.getType(), compiler, exp.getType());
            if (one != null && doCall != null) {
                ClosureUtil.makeOneMethodClass(expr.getType(), exp.getType(), one, doCall);

                if (exp.getExpression() instanceof VariableExpression) {
                    VariableExpression ve = (VariableExpression) exp.getExpression();
                    if (ve.isDynamicTyped()) {
                        compiler.getLocalVarInferenceTypes().addWeak(ve, expr.getType());
                    }
                }

                return expr;
            }
        }

        return standardCast(exp, compiler, expr);
    }

    private BytecodeExpr buildClassFromMap(MapExpression exp, ClassNode type, final CompilerTransformer compiler) {

        ClassNode objType = new ClassNode(compiler.getNextClosureName(), ACC_PUBLIC|ACC_FINAL, ClassHelper.OBJECT_TYPE);

        if (type.isInterface()) {
            objType.setInterfaces(new ClassNode [] {type} );
        }
        else {
            objType.setSuperClass(type);
        }

        objType.setModule(compiler.classNode.getModule());

        if (!compiler.methodNode.isStatic() || compiler.classNode.getName().endsWith("$TraitImpl"))
            objType.addField("$owner", Opcodes.ACC_PUBLIC, !compiler.methodNode.isStatic() ? compiler.classNode : compiler.methodNode.getParameters()[0].getType(), null);

        Set<String> fieldNames = new HashSet<String> ();

        final List list = exp.getMapEntryExpressions();
        for (int i = 0; i != list.size(); ++i) {
            final MapEntryExpression me = (MapEntryExpression) list.get(i);

            Expression key = me.getKeyExpression();
            if (!(key instanceof ConstantExpression) || !(((ConstantExpression)key).getValue() instanceof String)) {
                compiler.addError( "<key> must have java.lang.String type", key);
                return null;
            }

            String keyName = (String) ((ConstantExpression)key).getValue();

            Expression value = me.getValueExpression();
            if (value instanceof ClosureExpression) {
                ClosureExpression ce = (ClosureExpression) value;

                boolean addDefault = false;
                if (ce.getParameters() != null && ce.getParameters().length == 0) {
                    addDefault = true;
                    final VariableScope scope = ce.getVariableScope();
                    ce = new ClosureExpression(new Parameter[1], ce.getCode());
                    ce.setVariableScope(scope);
                    ce.getParameters()[0] = new Parameter(ClassHelper.OBJECT_TYPE, "it");
                }
                
                final ClosureMethodNode _doCallMethod = new ClosureMethodNode(
                        keyName,
                        Opcodes.ACC_PUBLIC,
                        ClassHelper.OBJECT_TYPE,
                        ce.getParameters() == null ? Parameter.EMPTY_ARRAY : ce.getParameters(),
                        ce.getCode());
                objType.addMethod(_doCallMethod);

                ClosureMethodNode defMethod = null;
                if (addDefault) {
                    defMethod = ClosureUtil.createDependentMethod(objType, _doCallMethod);
                }

                Object methods = ClassNodeCache.getMethods(type, keyName);
                if (methods != null) {
                    if (methods instanceof MethodNode) {
                        MethodNode baseMethod = (MethodNode) methods;
                        checkOveride (_doCallMethod, defMethod, baseMethod, type);
                    }
                    else {
                        FastArray methodsArr = (FastArray) methods;
                        int methodCount = methodsArr.size();
                        for (int j = 0; j != methodCount; ++j) {
                            MethodNode baseMethod = (MethodNode) methodsArr.get(j);
                            checkOveride (_doCallMethod, defMethod, baseMethod, type);
                        }
                    }
                }

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

                    if (!fieldNames.contains(astVar.getName())) {
                        fieldNames.add(astVar.getName());
                        objType.addField(astVar.getName(), ACC_FINAL, vtype, null);
                    }
                }

                StaticMethodBytecode.replaceMethodCode(compiler.su, _doCallMethod, compiler.compileStack, compiler.debug == -1 ? -1 : compiler.debug+1, compiler.policy, compiler.classNode.getName());
            }
            else {
                // @todo
            }
        }

        return new BytecodeExpr(exp, objType) {
            protected void compile(MethodVisitor mv) {
                ClosureUtil.instantiateClass(getType(), compiler, mv);
            }
        };
    }

    private void checkOveride(ClosureMethodNode callMethod, ClosureMethodNode defMethod, MethodNode baseMethod, ClassNode baseType) {
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

        Parameter[] baseMethodParameters = baseMethod.getParameters();
        Parameter[] closureParameters = callMethod.getParameters();

        boolean match = true;
        if (closureParameters.length == baseMethodParameters.length) {
            for (int i = 0; i < closureParameters.length; i++) {
                Parameter closureParameter = closureParameters[i];
                Parameter missingMethodParameter = baseMethodParameters[i];

                ClassNode parameterType = missingMethodParameter.getType();
                parameterType = TypeUtil.getSubstitutedType(parameterType, baseType.redirect(), baseType);
                if (!TypeUtil.isAssignableFrom(parameterType, closureParameter.getType())) {
                    if (TypeUtil.isAssignableFrom(closureParameter.getType(), parameterType)) {
                        if (mutations == null)
                            mutations = new LinkedList<Mutation>();
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
                ClassNode returnType = TypeUtil.getSubstitutedType(baseMethod.getReturnType(), baseType.redirect(), baseType);
                callMethod.setReturnType(returnType);
                return;
            }
        }

        if (defMethod != null && baseMethodParameters.length == 0) {
            ClassNode returnType = TypeUtil.getSubstitutedType(baseMethod.getReturnType(), baseType.redirect(), baseType);
            callMethod.setReturnType(returnType);
        }
    }

    private ClassNode calcResultCollectionType(CastExpression exp, ClassNode componentType, CompilerTransformer compiler) {
        ClassNode collType = exp.getType();
        if ((collType.getModifiers() & ACC_ABSTRACT) != 0) {
            if (collType.equals(ClassHelper.LIST_TYPE)) {
                if (collType.getGenericsTypes() != null) {
                    collType = ClassHelper.make ("java.util.ArrayList");
                    collType.setRedirect(TypeUtil.ARRAY_LIST_TYPE);
                    collType.setGenericsTypes(new GenericsType[]{new GenericsType(componentType)});
                }
                else
                    collType = TypeUtil.ARRAY_LIST_TYPE;
            }
            else {
                if (collType.equals(TypeUtil.SET_TYPE)) {
                    if (collType.getGenericsTypes() != null) {
                        collType = ClassHelper.make ("java.util.LinkedHashMap");
                        collType.setRedirect(TypeUtil.LINKED_HASH_MAP_TYPE);
                        collType.setGenericsTypes(new GenericsType[]{new GenericsType(componentType)});
                    }
                    else
                        collType = TypeUtil.LINKED_HASH_SET_TYPE;
                }
                else {
                    if (collType.equals(TypeUtil.SORTED_SET_TYPE)) {
                        if (collType.getGenericsTypes() != null) {
                            collType = ClassHelper.make ("java.util.TreeSet");
                            collType.setRedirect(TypeUtil.TREE_SET_TYPE);
                            collType.setGenericsTypes(new GenericsType[]{new GenericsType(componentType)});
                        }
                        else
                            collType = TypeUtil.TREE_SET_TYPE;
                    }
                    else {
                        if (collType.equals(TypeUtil.QUEUE_TYPE)) {
                            if (collType.getGenericsTypes() != null) {
                                collType = ClassHelper.make ("java.util.LinkedList");
                                collType.setRedirect(TypeUtil.LINKED_LIST_TYPE);
                                collType.setGenericsTypes(new GenericsType[]{new GenericsType(componentType)});
                            }
                            else
                                collType = TypeUtil.LINKED_LIST_TYPE;
                        }
                        else {
                            compiler.addError ("Can't instantiate list as instance of abstract type " + collType.getName(), exp);
                            return null;
                        }
                    }
                }
            }
        }
        return collType;
    }

    private BytecodeExpr listToArray(CastExpression exp, CompilerTransformer compiler) {
        return standardCast(exp, compiler, (BytecodeExpr) compiler.transform(exp.getExpression()));
    }

    private BytecodeExpr standardCast(final CastExpression exp, CompilerTransformer compiler, final BytecodeExpr expr) {
        if (exp.isCoerce()) {
            // a)
            final ClassNode type = TypeUtil.wrapSafely(exp.getType());
            Expression arg = ClassExpressionTransformer.newExpr(exp, type);
            return new AsType(exp, type, expr, (BytecodeExpr) arg);
        } else {
            if (TypeUtil.isNumericalType(exp.getType()) && TypeUtil.isNumericalType(expr.getType())) {
                // b)
                return new Cast(exp.getType(), expr);
            } else {
                ClassNode rtype = TypeUtil.wrapSafely(expr.getType());
                if (rtype.equals(TypeUtil.NULL_TYPE) && ClassHelper.isPrimitiveType(exp.getType())) {
                    return new BytecodeExpr(exp, exp.getType()) {
                        protected void compile(MethodVisitor mv) {
                            if (exp.getType() == ClassHelper.double_TYPE) {
                                mv.visitLdcInsn((double)0);
                            } else if (exp.getType() == ClassHelper.float_TYPE) {
                                mv.visitLdcInsn((float)0);
                                mv.visitInsn(FRETURN);
                            } else if (exp.getType() == ClassHelper.long_TYPE) {
                                mv.visitLdcInsn(0L);
                                mv.visitInsn(LRETURN);
                            } else
                                mv.visitInsn(ICONST_0);
                            }
                    };
                }

                if (TypeUtil.isDirectlyAssignableFrom(exp.getType(), rtype)) {
                    // c)
                    if (rtype.equals(exp.getType())) {
                        expr.setType(exp.getType()); // important for correct generic signature
                        return expr;
                    }
                    else {
                        return new BytecodeExpr(expr, exp.getType()) {
                            protected void compile(MethodVisitor mv) {
                                expr.visit(mv);
                                box(expr.getType(), mv);
                            }
                        };
                    }
                } else {
                    // d
                    if (exp.getExpression() instanceof VariableExpression) {
                        VariableExpression ve = (VariableExpression) exp.getExpression();
                        if (ve.isDynamicTyped()) {
                            compiler.getLocalVarInferenceTypes().addWeak(ve, expr.getType());
                        }
                    }

                    return new Cast(exp.getType(), expr);
                }
            }
        }
    }

    private void improveListTypes(ListExpression listExpression, ClassNode componentType) {
        List<Expression> list = listExpression.getExpressions();
        int count = list.size();
        for (int i = 0; i != count; ++i) {
            Expression el = list.get(i);
            CastExpression castExpression = new CastExpression(componentType, el);
            castExpression.setSourcePosition(el);
            list.set(i, castExpression);
        }
    }

    private static class AsType extends BytecodeExpr {
        private final BytecodeExpr expr;
        private final BytecodeExpr arg1;

        public AsType(CastExpression exp, ClassNode type, BytecodeExpr expr, BytecodeExpr arg1) {
            super(exp, type);
            this.expr = expr;
            this.arg1 = arg1;
        }

        protected void compile(MethodVisitor mv) {
            expr.visit(mv);
            box(expr.getType(), mv);
            arg1.visit(mv);
            mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/ScriptBytecodeAdapter", "asType", "(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;");
            BytecodeExpr.checkCast(getType(), mv);
        }
    }

    public static class Cast extends BytecodeExpr {
        private final BytecodeExpr expr;

        public Cast(ClassNode type, BytecodeExpr expr) {
            super(expr, type);
            this.expr = expr;
        }

        protected void compile(MethodVisitor mv) {
            expr.visit(mv);
            box(expr.getType(), mv);
            cast(TypeUtil.wrapSafely(expr.getType()), TypeUtil.wrapSafely(getType()), mv);
            unbox(getType(), mv);
        }
    }
}
