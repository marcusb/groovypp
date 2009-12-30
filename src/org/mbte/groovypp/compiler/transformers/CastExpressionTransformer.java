package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.util.FastArray;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.*;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.PropertyUtil;
import org.mbte.groovypp.compiler.bytecode.ResolvedMethodBytecodeExpr;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;

/**
 * Cast processing rules:
 * a) as operator always go via asType method
 * b) both numerical types always goes via primitive types
 * c) if we cast statically to lower type we keep upper (except if upper NullType)
 * d) otherwise we do direct cast
 * e) primitive types goes via boxing and Number
 */
public class CastExpressionTransformer extends ExprTransformer<CastExpression> {
    public BytecodeExpr transform(CastExpression cast, CompilerTransformer compiler) {

        if (cast.getExpression() instanceof TernaryExpression) {
            return compiler.cast(cast.getExpression(), cast.getType());
        }

        if (cast.getExpression() instanceof ListExpressionTransformer.UntransformedListExpr) {
            final CastExpression newExp = new CastExpression(cast.getType(), ((ListExpressionTransformer.UntransformedListExpr) cast.getExpression()).exp);
            newExp.setSourcePosition(cast);
            cast = newExp;
        }

        if (cast.getExpression() instanceof MapExpressionTransformer.UntransformedMapExpr) {
            final CastExpression newExp = new CastExpression(cast.getType(), ((MapExpressionTransformer.UntransformedMapExpr) cast.getExpression()).exp);
            newExp.setSourcePosition(cast);
            cast = newExp;
        }

        if (cast.getType().equals(ClassHelper.boolean_TYPE) || cast.getType().equals(ClassHelper.Boolean_TYPE)) {
            if (cast.getExpression() instanceof ListExpression) {
                return compiler.castToBoolean( new ListExpressionTransformer.TransformedListExpr( (ListExpression)cast.getExpression(), TypeUtil.ARRAY_LIST_TYPE, compiler), cast.getType());
            }
            if (cast.getExpression() instanceof MapExpression) {
                return compiler.castToBoolean( new MapExpressionTransformer.TransformedMapExpr( (MapExpression)cast.getExpression(), compiler), cast.getType());
            }
            return compiler.castToBoolean((BytecodeExpr)compiler.transform(cast.getExpression()), cast.getType());
        }

        if (cast.getType().equals(ClassHelper.STRING_TYPE)) {
            if (cast.getExpression() instanceof ListExpression) {
                return compiler.castToBoolean( new ListExpressionTransformer.TransformedListExpr( (ListExpression)cast.getExpression(), TypeUtil.ARRAY_LIST_TYPE, compiler), cast.getType());
            }
            return compiler.castToString((BytecodeExpr)compiler.transform(cast.getExpression()), cast.getType());
        }

        if (cast.getExpression() instanceof ListExpression) {
            ListExpression listExpression = (ListExpression) cast.getExpression();

            if (cast.getType().isArray()) {
                ClassNode componentType = cast.getType().getComponentType();
                improveListTypes(listExpression, componentType);
                final ArrayExpression array = new ArrayExpression(componentType, listExpression.getExpressions(), null);
                array.setSourcePosition(listExpression);
                return (BytecodeExpr) compiler.transform(array);
            }

            if(cast.getType().implementsInterface(TypeUtil.ITERABLE) || cast.getType().equals(TypeUtil.ITERABLE)) {
                if(compiler.findConstructor(cast.getType(), ClassNode.EMPTY_ARRAY) != null){
                    ClassNode componentType = compiler.getCollectionType(cast.getType());
                    improveListTypes(listExpression, componentType);
                    final List list = listExpression.getExpressions();
                    for (int i = 0; i != list.size(); ++i) {
                        list.set(i, compiler.transform((Expression) list.get(i)));
                    }

                    ClassNode collType = calcResultCollectionType(cast, componentType, compiler);
                    return new ListExpressionTransformer.TransformedListExpr(listExpression, collType, compiler);
                }
            }

            if (!TypeUtil.isDirectlyAssignableFrom(cast.getType(), TypeUtil.ARRAY_LIST_TYPE)) {
                final ConstructorCallExpression constr = new ConstructorCallExpression(cast.getType(), new ArgumentListExpression(listExpression.getExpressions()));
                constr.setSourcePosition(cast);
                return (BytecodeExpr) compiler.transform(constr);
            }
            else {
                // Assignable from ArrayList but not Iterable
                ClassNode componentType = ClassHelper.OBJECT_TYPE;
                improveListTypes(listExpression, componentType);
                final List list = listExpression.getExpressions();
                for (int i = 0; i != list.size(); ++i) {
                    list.set(i, compiler.transform((Expression) list.get(i)));
                }

                ClassNode collType = TypeUtil.ARRAY_LIST_TYPE;
                return new ListExpressionTransformer.TransformedListExpr(listExpression, collType, compiler);
            }
        }

        if (cast.getExpression() instanceof MapExpression) {
            MapExpression mapExpression = (MapExpression) cast.getExpression();

            if (!cast.getType().implementsInterface(ClassHelper.MAP_TYPE)
             && !cast.getType().equals(ClassHelper.MAP_TYPE)
             && !TypeUtil.isAssignableFrom(cast.getType(), TypeUtil.LINKED_HASH_MAP_TYPE)) {
                return buildClassFromMap (mapExpression, cast.getType(), compiler);
            }
            else {
                final MapExpressionTransformer.TransformedMapExpr inner = new MapExpressionTransformer.TransformedMapExpr((MapExpression) cast.getExpression(), compiler);
                return standardCast(cast, compiler, inner);
            }
        }

        BytecodeExpr expr = (BytecodeExpr) compiler.transform(cast.getExpression());

        if (expr.getType().implementsInterface(TypeUtil.TCLOSURE)) {
            List<MethodNode> one = ClosureUtil.isOneMethodAbstract(cast.getType());
            MethodNode doCall = one == null ? null : ClosureUtil.isMatch(one, (ClosureClassNode) expr.getType(), compiler, cast.getType());
            if (one != null && doCall != null) {
                ClosureUtil.makeOneMethodClass(expr.getType(), cast.getType(), one, doCall);

                if (cast.getExpression() instanceof VariableExpression) {
                    VariableExpression ve = (VariableExpression) cast.getExpression();
                    if (ve.isDynamicTyped()) {
                        compiler.getLocalVarInferenceTypes().addWeak(ve, expr.getType());
                    }
                }

                return expr;
            }
        }

        if (!TypeUtil.isDirectlyAssignableFrom(cast.getType(), expr.getType())) {
            MethodNode unboxing = TypeUtil.getReferenceUnboxingMethod(expr.getType());
            if (unboxing != null) {
                expr = ResolvedMethodBytecodeExpr.create(cast, unboxing, expr, new ArgumentListExpression(), compiler);
            }
        }

        return standardCast(cast, compiler, expr);
    }

    private BytecodeExpr buildClassFromMap(MapExpression exp, ClassNode type, final CompilerTransformer compiler) {

        final List list = exp.getMapEntryExpressions();

        Expression superArgs = null;

        for (int i = 0; i != list.size(); ++i) {
            final MapEntryExpression me = (MapEntryExpression) list.get(i);

            Expression key = me.getKeyExpression();
            if (!(key instanceof ConstantExpression) || !(((ConstantExpression)key).getValue() instanceof String)) {
                compiler.addError( "<key> must have java.lang.String type", key);
                return null;
            }
        }

        ClassNode objType = null;

        if ((type.getModifiers() & ACC_ABSTRACT) != 0) {
            objType = createNewType(type, compiler);
        }

        List<MapEntryExpression> methods = new LinkedList<MapEntryExpression>();
        final List<MapEntryExpression> fields = new LinkedList<MapEntryExpression>();

        for (int i = 0; i != list.size(); ++i) {
            final MapEntryExpression me = (MapEntryExpression) list.get(i);

            String keyName = (String) ((ConstantExpression)me.getKeyExpression()).getValue();
            Expression value = me.getValueExpression();

            if (keyName.equals("super")) {
                if (objType == null)
                    objType = createNewType(type, compiler);
                superArgs = value;
                continue;
            }

            final Object prop = PropertyUtil.resolveSetProperty(type, keyName, TypeUtil.NULL_TYPE, compiler);
            if (prop != null) {
                ClassNode propType = null;
                if (prop instanceof MethodNode)
                    propType = ((MethodNode)prop).getParameters()[0].getType();
                else
                    if (prop instanceof FieldNode)
                        propType = ((FieldNode)prop).getType();
                    else
                        propType = ((PropertyNode)prop).getType();

                final CastExpression cast = new CastExpression(propType, value);
                cast.setSourcePosition(value);
                exp.getMapEntryExpressions().set(i, new MapEntryExpression(me.getKeyExpression(), cast));
            }
            else {
                if (objType == null)
                    objType = createNewType(type, compiler);

                if (value instanceof ClosureExpression) {
                    ClosureExpression ce = (ClosureExpression) value;

                    methods.add (me);

                    ClosureUtil.addFields(ce, objType, compiler);
                }
                else {
                    fields.add(me);
                }
            }
        }

        if (objType != null) {
            final Parameter[] constrParams = ClosureUtil.createClosureConstructorParams(objType, compiler);

            if (superArgs != null) {
                if (superArgs instanceof ListExpression) {
                    superArgs = new ArgumentListExpression(((ListExpression)superArgs).getExpressions());
                }
                else
                    superArgs = new ArgumentListExpression(superArgs);
            }
            else
                superArgs = new ArgumentListExpression();
            superArgs = compiler.transform(superArgs);

            final MethodNode constructor = ConstructorCallExpressionTransformer.findConstructorWithClosureCoercion(objType.getSuperClass(), compiler.exprToTypeArray(superArgs), compiler);

            if (constructor == null) {
                compiler.addError ("Can't find super constructor", objType);
                return null;
            }

            final List<Expression> ll = ((ArgumentListExpression) superArgs).getExpressions();
            for (int i = 0; i != ll.size(); ++i)
                ll.set(i, compiler.cast(ll.get(i), constructor.getParameters()[i].getType()));

            ClosureUtil.createClosureConstructor(objType, constrParams, superArgs);

            for (MapEntryExpression me : fields) {
                final String keyName = (String) ((ConstantExpression) me.getKeyExpression()).getValue();

                final Expression init = compiler.transform(me.getValueExpression());

                me.setValueExpression(init);

                objType.addField(keyName, 0, init.getType(), null);
            }

            for (MapEntryExpression me : methods) {
                final String keyName = (String) ((ConstantExpression) me.getKeyExpression()).getValue();
                closureToMethod(type, compiler, objType, keyName, (ClosureExpression)me.getValueExpression());
            }

            final Expression finalSA = superArgs;
            return new BytecodeExpr(exp, objType) {
                protected void compile(MethodVisitor mv) {
                    ClosureUtil.instantiateClass(getType(), compiler, constrParams, finalSA, mv);

                    for (MapEntryExpression me : fields) {
                        final String keyName = (String) ((ConstantExpression) me.getKeyExpression()).getValue();

                        final FieldNode fieldNode = getType().getDeclaredField(keyName);

                        mv.visitInsn(DUP);
                        ((BytecodeExpr)me.getValueExpression()).visit(mv);
                        mv.visitFieldInsn(PUTFIELD, BytecodeHelper.getClassInternalName(getType()), fieldNode.getName(), BytecodeHelper.getTypeDescription(fieldNode.getType()));
                    }
                }
            };
        }
        else {
            final ConstructorCallExpression constr = new ConstructorCallExpression(type, new ArgumentListExpression(exp));
            constr.setSourcePosition(exp);
            return (BytecodeExpr) compiler.transform(constr);
        }
    }

    private void closureToMethod(ClassNode type, CompilerTransformer compiler, ClassNode objType, String keyName, ClosureExpression ce) {
        if (ce.getParameters() != null && ce.getParameters().length == 0) {
            final VariableScope scope = ce.getVariableScope();
            ce = new ClosureExpression(new Parameter[1], ce.getCode());
            ce.setVariableScope(scope);
            ce.getParameters()[0] = new Parameter(ClassHelper.OBJECT_TYPE, "it", new ConstantExpression(null));
        }

        final ClosureMethodNode _doCallMethod = new ClosureMethodNode(
                keyName,
                Opcodes.ACC_PUBLIC,
                ClassHelper.OBJECT_TYPE,
                ce.getParameters() == null ? Parameter.EMPTY_ARRAY : ce.getParameters(),
                ce.getCode());
        objType.addMethod(_doCallMethod);

        _doCallMethod.createDependentMethods(objType);

        Object methods = ClassNodeCache.getMethods(type, keyName);
        if (methods != null) {
            if (methods instanceof MethodNode) {
                MethodNode baseMethod = (MethodNode) methods;
                _doCallMethod.checkOveride(baseMethod, type);
            }
            else {
                FastArray methodsArr = (FastArray) methods;
                int methodCount = methodsArr.size();
                for (int j = 0; j != methodCount; ++j) {
                    MethodNode baseMethod = (MethodNode) methodsArr.get(j);
                    _doCallMethod.checkOveride(baseMethod, type);
                }
            }
        }

        ClassNodeCache.clearCache (_doCallMethod.getDeclaringClass());
        StaticMethodBytecode.replaceMethodCode(compiler.su, compiler.context, _doCallMethod, compiler.compileStack, compiler.debug == -1 ? -1 : compiler.debug+1, compiler.policy, compiler.classNode.getName());
    }

    private ClassNode createNewType(ClassNode type, CompilerTransformer compiler) {
        ClassNode objType;
        objType = new ClassNode(compiler.getNextClosureName(), ACC_PUBLIC|ACC_FINAL|ACC_SYNTHETIC, ClassHelper.OBJECT_TYPE);
        if (type.isInterface()) {
            objType.setInterfaces(new ClassNode [] {type} );
        }
        else {
            objType.setSuperClass(type);
        }
        objType.setModule(compiler.classNode.getModule());

        if (!compiler.methodNode.isStatic() || compiler.classNode.getName().endsWith("$TraitImpl"))
            objType.addField("this$0", ACC_PUBLIC|ACC_FINAL|ACC_SYNTHETIC, !compiler.methodNode.isStatic() ? compiler.classNode : compiler.methodNode.getParameters()[0].getType(), null);

        return objType;
    }

    private ClassNode calcResultCollectionType(CastExpression exp, ClassNode componentType, CompilerTransformer compiler) {
        ClassNode collType = exp.getType();
        if ((collType.getModifiers() & ACC_ABSTRACT) != 0) {
            if (collType.equals(ClassHelper.LIST_TYPE) || collType.equals(TypeUtil.COLLECTION_TYPE)) {
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
                        collType = ClassHelper.make ("java.util.LinkedHashSet");
                        collType.setRedirect(TypeUtil.LINKED_HASH_SET_TYPE);
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
                                mv.visitInsn(DCONST_0);
                            } else if (exp.getType() == ClassHelper.float_TYPE) {
                                mv.visitInsn(FCONST_0);
                            } else if (exp.getType() == ClassHelper.long_TYPE) {
                                mv.visitInsn(LCONST_0);
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
            if (!(el instanceof BytecodeSpreadExpr) && !(el instanceof SpreadExpression)) {
                CastExpression castExpression = new CastExpression(componentType, el);
                castExpression.setSourcePosition(el);
                list.set(i, castExpression);
            }
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
