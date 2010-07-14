/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.groovypp.compiler;

import groovy.lang.TypePolicy;
import groovy.lang.Use;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.util.FastArray;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.LocalVarTypeInferenceState;
import org.mbte.groovypp.compiler.bytecode.StackAwareMethodAdapter;
import org.mbte.groovypp.compiler.transformers.ExprTransformer;
import org.mbte.groovypp.compiler.transformers.ListExpressionTransformer;
import org.mbte.groovypp.compiler.transformers.MapExpressionTransformer;
import org.mbte.groovypp.compiler.transformers.TernaryExpressionTransformer;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.LinkedList;
import java.util.List;

import static org.mbte.groovypp.compiler.transformers.ExprTransformer.transformExpression;

public abstract class CompilerTransformer extends ReturnsAdder implements Opcodes, LocalVarTypeInferenceState {

    public final CompilerStack compileStack;
    public final ClassNode classNode;
    protected final StackAwareMethodAdapter mv;
    public final int debug;
    public final TypePolicy policy;
    private static final ClassNode USE = ClassHelper.make(Use.class);
    private int nestedLevel;
    LinkedList<CompiledClosureBytecodeExpr> pendingClosures = new LinkedList<CompiledClosureBytecodeExpr> ();
    private int nextClosureIndex = 1;
    private final String baseClosureName;
    public SourceUnitContext context;

    public CompilerTransformer(SourceUnit source, ClassNode classNode, MethodNode methodNode,
                               StackAwareMethodAdapter mv, CompilerStack compileStack, int debug,
                               TypePolicy policy, String baseClosureName, SourceUnitContext context) {
        super(source, methodNode);
        this.classNode = classNode;
        this.mv = mv;
        this.debug = debug;
        this.policy = policy;
        this.baseClosureName = baseClosureName;
        this.compileStack = new CompilerStack(compileStack);
        this.context = context;
    }

    public void addError(String msg, ASTNode expr) {
        int line = expr.getLineNumber();
        int col = expr.getColumnNumber();
        SourceUnit source = getSourceUnit();
        source.getErrorCollector().addError(
                new SyntaxErrorMessage(new SyntaxException(msg + '\n', line, col), source), true
        );
    }

    @Override
    public Expression transform(Expression exp) {
        nestedLevel++;
        try {
            Expression result = transformExpression(exp, this);
            processPendingClosures();
            return result;
        }
        catch (MultipleCompilationErrorsException err) {
            throw err;
        }
        catch (Throwable e) {
            e.printStackTrace();
            addError(e.getMessage(), exp);
            return null;
        }
        finally {
            nestedLevel--;
        }
    }
    
    public Expression transformToGround(Expression expr) {
        return transformSynthetic((BytecodeExpr) transform(expr));
    }

    public BytecodeExpr transformSynthetic(BytecodeExpr res) {
        if (res instanceof ListExpressionTransformer.UntransformedListExpr)
            return ((ListExpressionTransformer.UntransformedListExpr) res).transform(TypeUtil.ARRAY_LIST_TYPE, this);
        else if (res instanceof MapExpressionTransformer.UntransformedMapExpr)
            return ((MapExpressionTransformer.UntransformedMapExpr) res).transform(this);
        else if (res instanceof TernaryExpressionTransformer.UntransformedTernaryExpr)
            return ((TernaryExpressionTransformer.UntransformedTernaryExpr) res).transform(this);
        else if (res.getType().declaresInterface(TypeUtil.TTHIS)) {
            res.setType(res.getType().getOuterClass());
        }
        return res;
    }

    private void processPendingClosures() {
        if (nestedLevel == 1) {
            if (!pendingClosures.isEmpty()) {
                for (CompiledClosureBytecodeExpr pendingClosure : pendingClosures) {
                    processPendingClosure(pendingClosure);
                }
                pendingClosures.clear();
            }
        }
    }

    public void processPendingClosure(CompiledClosureBytecodeExpr pendingClosure) {
        ClosureClassNode type = (ClosureClassNode) pendingClosure.getType();
        ClosureMethodNode doCallMethod = type.getDoCallMethod();
        Statement code = doCallMethod.getCode();
        if (!(code instanceof BytecodeSequence)) {
            ClosureUtil.improveClosureType(type, ClassHelper.CLOSURE_TYPE);
            StaticMethodBytecode.replaceMethodCode(su, context, doCallMethod, compileStack, debug == -1 ? -1 : debug+1, policy, type.getName());
        }
    }

    public BytecodeExpr transformLogical(Expression exp, Label label, boolean onTrue) {
        nestedLevel++;
        try {
            final BytecodeExpr result = ExprTransformer.transformLogicalExpression(exp, this, label, onTrue);
            processPendingClosures();
            return result;
        }
        catch (MultipleCompilationErrorsException err) {
            throw err;
        }
        catch (Throwable e) {
            e.printStackTrace();
            addError(e.getMessage(), exp);
            return null;
        }
        finally {
            nestedLevel--;
        }
    }

    public Expression transformImpl(Expression exp) {
        if (exp instanceof SpreadMapExpression) {
            addError("Spread expressions are not supported by static compiler", exp);
            return null;
        }

        if (exp instanceof StaticMethodCallExpression) {
            StaticMethodCallExpression smce = (StaticMethodCallExpression) exp;
            MethodCallExpression mce = new MethodCallExpression(
                    new ClassExpression(smce.getOwnerType()),
                    new ConstantExpression(smce.getMethod()),
                    smce.getArguments());
            mce.setSourcePosition(smce);
            mce.getMethod().setSourcePosition(smce);
            return transform(mce);
        }

        return super.transform(exp);
    }

    public FieldNode findField(ClassNode type, String fieldName) {
        Object fields = ClassNodeCache.getFields(type, fieldName);
        return (FieldNode) fields;
    }

    private Object findCategoryMethod(ClassNode category, String methodName, ClassNode objectType, ClassNode [] args, Object candidates) {
        final Object o = ClassNodeCache.getMethods(category, methodName);
        if (o instanceof MethodNode) {
            MethodNode mn = (MethodNode) o;
            if (mn.isStatic()) {
                final Parameter[] parameters = mn.getParameters();
                if (parameters.length > 0 && TypeUtil.isDirectlyAssignableFrom(parameters[0].getType(), objectType)) {
                    candidates = ClassNodeCache.createDGM(mn);
                }
            }
        } else {
            FastArray ms = (FastArray) o;
            if (ms == null) return candidates;
            for (int i = 0; i != ms.size(); ++i) {
                MethodNode mn = (MethodNode) ms.get(i);
                if (mn.isStatic()) {
                    final Parameter[] parameters = mn.getParameters();
                    if (parameters.length > 0) {
                        if (TypeUtil.isDirectlyAssignableFrom(parameters[0].getType(), objectType)) {
                            if (candidates == null)
                                candidates = ClassNodeCache.createDGM(mn);
                            else if (candidates instanceof FastArray) {
                                ((FastArray) candidates).add(ClassNodeCache.createDGM(mn));
                            } else {
                                MethodNode _1st = (MethodNode) candidates;
                                candidates = new FastArray(2);
                                ((FastArray) candidates).add(_1st);
                                ((FastArray) candidates).add(ClassNodeCache.createDGM(mn));
                            }
                        }
                    }
                }
            }
        }

        return candidates;
    }

    public MethodNode findMethod(ClassNode type, String methodName, ClassNode[] args, boolean staticOnly) {
        Object methods = staticOnly ? ClassNodeCache.getStaticMethods(type, methodName) :
                ClassNodeCache.getMethods(type, methodName);
        final Object res = MethodSelection.chooseMethod(methodName, methods, type, args, classNode);
        if (res instanceof MethodNode)
            return (MethodNode) res;

        if (!staticOnly) {
            Object candidates = findCategoryMethod(type, methodName, type, args, null);

            if (candidates == null) {
                candidates = findCategoryMethod(classNode, methodName, type, args, candidates);
                final List<AnnotationNode> list = classNode.getAnnotations(USE);
                for (AnnotationNode annotationNode : list) {
                    final Expression member = annotationNode.getMember("value");
                    if (member instanceof ClassExpression) {
                        ClassExpression expression = (ClassExpression) member;
                        final ClassNode category = expression.getType();
                        candidates = findCategoryMethod(category, methodName, type, args, candidates);
                    }
                }
            }

            if (candidates == null) {
                final CompileUnit compileUnit = classNode.getCompileUnit();
                if (compileUnit != null)
                    for (ModuleNode moduleNode : compileUnit.getModules()) {
                        for (ClassNode category : moduleNode.getClasses()) {
                            candidates = findCategoryMethod(category, methodName, type, args, candidates);
                        }
                    }
            }

            if (candidates != null) {
                final Object r = MethodSelection.chooseMethod(methodName, candidates, type, args, classNode);
                if (r instanceof MethodNode)
                    return (MethodNode) r;
            }
        }

        return null;
    }

    public PropertyNode findProperty(ClassNode type, String property) {
        for (; type != null; type = type.getSuperClass()) {
            PropertyNode propertyNode = type.getProperty(property);
            if (propertyNode != null)
                return propertyNode;
        }
        return null;
    }

    public MethodNode findConstructor(ClassNode type, ClassNode[] args, ClassNode contextClass) {
        FastArray methods = ClassNodeCache.getConstructors(type);

//        if (type.redirect() instanceof InnerClassNode && (type.getModifiers() & ACC_STATIC) == 0) {
//            ClassNode newArgs [] = new ClassNode[args.length+1];
//
//            for (ClassNode tp = classNode ; tp != null && !tp.equals(type.redirect().getOuterClass()); ) {
//                final ClassNode outerTp = tp.getOuterClass();
//
//                tp = outerTp;
//            }
//
//            newArgs [0] = type.getOuterClass();
//            System.arraycopy(args, 0, newArgs, 1, args.length);
//            args = newArgs;
//        }

        final Object res = MethodSelection.chooseMethod("<init>", methods, type, args, contextClass == null ? classNode : contextClass);
        if (res instanceof MethodNode)
            return (MethodNode) res;
        return null;
    }

    public ClassNode[] exprToTypeArray(Expression args) {
        final List list = ((TupleExpression) args).getExpressions();
        final ClassNode[] nodes = new ClassNode[list.size()];
        for (int i = 0; i < nodes.length; i++) {
            ClassNode type = ((Expression) list.get(i)).getType();
            if (type == TypeUtil.NULL_TYPE)
                nodes[i] = null;
            else
                nodes[i] = type;
        }
        return nodes;
    }

    public void mathOp(ClassNode type, Token op, BinaryExpression be) {
        switch (op.getType()) {
            case Types.PLUS:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(IADD);
                else if (type == ClassHelper.double_TYPE)
                    mv.visitInsn(DADD);
                else if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LADD);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.COMPARE_NOT_EQUAL: {
                Label _true = new Label();
                if (type == ClassHelper.int_TYPE)
                    mv.visitJumpInsn(IF_ICMPEQ, _true);
                else if (type == ClassHelper.double_TYPE) {
                    mv.visitInsn(DCMPG);
                    mv.visitJumpInsn(IFEQ, _true);
                } else if (type == ClassHelper.float_TYPE) {
                    mv.visitInsn(FCMPG);
                    mv.visitJumpInsn(IFEQ, _true);
                } else if (type == ClassHelper.long_TYPE) {
                    mv.visitInsn(LCMP);
                    mv.visitJumpInsn(IFEQ, _true);
                } else
                    throw new RuntimeException("Internal Error");
                mv.visitInsn(ICONST_1);
                Label _false = new Label();
                mv.visitJumpInsn(GOTO, _false);
                mv.visitLabel(_true);
                mv.visitInsn(ICONST_0);
                mv.visitLabel(_false);
                break;
            }

            case Types.MULTIPLY:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(IMUL);
                else if (type == ClassHelper.double_TYPE)
                    mv.visitInsn(DMUL);
                else if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LMUL);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.MINUS:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(ISUB);
                else if (type == ClassHelper.double_TYPE)
                    mv.visitInsn(DSUB);
                else if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LSUB);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.DIVIDE:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(IDIV);
                else if (type == ClassHelper.double_TYPE)
                    mv.visitInsn(DDIV);
                else if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LDIV);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.BITWISE_XOR:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(IXOR);
                else if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LXOR);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.BITWISE_AND:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(IAND);
                else if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LAND);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.INTDIV:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(IDIV);
                else if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LDIV);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.LEFT_SHIFT:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(ISHL);
                else if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LSHL);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.RIGHT_SHIFT:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(ISHR);
                else if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LSHR);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.RIGHT_SHIFT_UNSIGNED:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(IUSHR);
                else if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LUSHR);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.MOD:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(IREM);
                else if (type == ClassHelper.double_TYPE)
                    mv.visitInsn(DREM);
                else if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LREM);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.BITWISE_OR:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(IOR);
                else if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LOR);
                else
                    throw new RuntimeException("Internal Error");
                break;


            default:
                addError("Operation " + op.getDescription() + " doesn't supported", be);
        }
    }

    public BytecodeExpr cast(final Expression be, final ClassNode type) {

        if (be instanceof TernaryExpression) {
            TernaryExpression ternaryExpression = (TernaryExpression) be;
            TernaryExpression cast = new TernaryExpression(ternaryExpression.getBooleanExpression(),
                    cast(ternaryExpression.getTrueExpression(), type),
                    cast(ternaryExpression.getFalseExpression(), type));
            cast.setSourcePosition(be);
            return (BytecodeExpr) transform(cast);
        }
        
        final CastExpression cast = new CastExpression(type, be);
        cast.setSourcePosition(be);
        return (BytecodeExpr) transform(cast);
    }

    public BytecodeExpr castToBoolean(final BytecodeExpr be, final ClassNode type) {
        if (be.getType().equals(ClassHelper.boolean_TYPE))
            return be;

        if (ClassHelper.isPrimitiveType(be.getType())) {
            return new BytecodeExpr(be, type) {
                protected void compile(MethodVisitor mv) {
                    ClassNode btype = be.getType();
                    if (btype == ClassHelper.long_TYPE) {
                        be.visit(mv);
                        mv.visitInsn(L2I);
                    } else if (btype == ClassHelper.float_TYPE) {
                        mv.visitInsn(ICONST_0);
                        be.visit(mv);
                        mv.visitInsn(FCONST_0);
                        mv.visitInsn(FCMPG);
                        final Label falseL = new Label();
                        mv.visitJumpInsn(IFEQ, falseL);
                        mv.visitInsn(POP);
                        mv.visitInsn(ICONST_1);
                        mv.visitLabel(falseL);
                    } else if (btype == ClassHelper.double_TYPE) {
                        mv.visitInsn(ICONST_0);
                        be.visit(mv);
                        mv.visitInsn(DCONST_0);
                        mv.visitInsn(DCMPG);
                        final Label falseL = new Label();
                        mv.visitJumpInsn(IFEQ, falseL);
                        mv.visitInsn(POP);
                        mv.visitInsn(ICONST_1);
                        mv.visitLabel(falseL);
                    } else {
                        be.visit(mv);
                    }

                    if (type.equals(ClassHelper.Boolean_TYPE))
                        box(ClassHelper.boolean_TYPE, mv);
                }
            };
        }
        else {
            MethodCallExpression safeCall = new MethodCallExpression(new BytecodeExpr(be, be.getType()) {
            protected void compile(MethodVisitor mv) {
            }        }, "asBoolean", ArgumentListExpression.EMPTY_ARGUMENTS);
            safeCall.setSourcePosition(be);

            final BytecodeExpr call = (BytecodeExpr) transform(safeCall);

            if (!call.getType().equals(ClassHelper.boolean_TYPE))
                addError("method asBoolean () should return 'boolean'", be);

            return new BytecodeExpr(be, type) {
                protected void compile(MethodVisitor mv) {
                    be.visit(mv);
                    mv.visitInsn(DUP);
                    Label nullLabel = new Label(), endLabel = new Label ();

                    mv.visitJumpInsn(IFNULL, nullLabel);

                    call.visit(mv);
                    mv.visitJumpInsn(GOTO, endLabel);

                    mv.visitLabel(nullLabel);
                    mv.visitInsn(POP);
                    mv.visitInsn(ICONST_0);

                    mv.visitLabel(endLabel);

                    if (type.equals(ClassHelper.Boolean_TYPE))
                        box(ClassHelper.boolean_TYPE, mv);
                }
            };
        }
    }

    public BytecodeExpr castToString(final BytecodeExpr be) {
        if (be.getType().equals(TypeUtil.NULL_TYPE) || be.getType().equals(ClassHelper.STRING_TYPE))
            return be;
        
        MethodCallExpression safeCall = new MethodCallExpression(new BytecodeExpr(be, TypeUtil.wrapSafely(be.getType())) {
        protected void compile(MethodVisitor mv) {
        }        }, "toString", ArgumentListExpression.EMPTY_ARGUMENTS);
        safeCall.setSourcePosition(be);

        final BytecodeExpr call = (BytecodeExpr) transform(safeCall);

        if (!call.getType().equals(ClassHelper.STRING_TYPE))
            addError("method toString () should return 'java.lang.String'", be);

        return new BytecodeExpr(be, ClassHelper.STRING_TYPE) {
            protected void compile(MethodVisitor mv) {
                be.visit(mv);
                box(be.getType(), mv);
                mv.visitInsn(DUP);
                Label nullLabel = new Label(), endLabel = new Label ();

                mv.visitJumpInsn(IFNULL, nullLabel);

                call.visit(mv);
                mv.visitJumpInsn(GOTO, endLabel);

                mv.visitLabel(nullLabel);
                mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(ClassHelper.STRING_TYPE));

                mv.visitLabel(endLabel);
            }
        };
    }

    public ClassNode getCollectionType(ClassNode type) {
        final GenericsType[] generics = TypeUtil.getSubstitutedType(TypeUtil.ITERABLE, TypeUtil.ITERABLE, type).getGenericsTypes();
        if (generics == null) return ClassHelper.OBJECT_TYPE;
        ClassNode substitutedType = generics[0].getType();
        return getCollOrMapGenericType(substitutedType);
    }

    public ClassNode getMapKeyType(ClassNode type) {
        final GenericsType[] generics = TypeUtil.getSubstitutedType(ClassHelper.MAP_TYPE, ClassHelper.MAP_TYPE, type).getGenericsTypes();
        if (generics == null) return ClassHelper.OBJECT_TYPE;
        ClassNode substitutedType = generics[0].getType();
        return getCollOrMapGenericType(substitutedType);
    }

    public ClassNode getMapValueType(ClassNode type) {
        final GenericsType[] generics = TypeUtil.getSubstitutedType(ClassHelper.MAP_TYPE, ClassHelper.MAP_TYPE, type).getGenericsTypes();
        if (generics == null) return ClassHelper.OBJECT_TYPE;
        ClassNode substitutedType = generics[1].getType();
        return getCollOrMapGenericType(substitutedType);
    }

    public ClassNode getCollOrMapGenericType(ClassNode substitutedType) {
        while (substitutedType.equals(ClassHelper.OBJECT_TYPE) && !substitutedType.isGenericsPlaceHolder() && substitutedType.getGenericsTypes() != null && substitutedType.getGenericsTypes().length != 0) {
            GenericsType genericsType = substitutedType.getGenericsTypes()[0];
            if (genericsType.isWildcard()) {
                substitutedType = genericsType.getUpperBounds()[0];
                if (substitutedType.equals(ClassHelper.OBJECT_TYPE) && substitutedType.getGenericsTypes() != null && substitutedType.getGenericsTypes().length != 0) {
                    genericsType = substitutedType.getGenericsTypes()[0];
                }
            }
            else {
                substitutedType = genericsType.getType();
            }
        }
        return substitutedType;
    }

    public String getNextClosureName() {
        while (true) {
            String name = baseClosureName + "$" + (nextClosureIndex++);
            if (checkNotExist(name))
                return name;
        }
    }

    private boolean checkNotExist (String name) {
        for (ClassNode node : classNode.getModule().getClasses()) {
            if (name.equals(node.getName())) {
                return false;
            }
        }
        return true;
    }

}
