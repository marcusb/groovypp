package org.mbte.groovypp.compiler;

import groovy.lang.TypePolicy;
import groovy.lang.Use;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.syntax.RuntimeParserException;
import org.codehaus.groovy.util.FastArray;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.LocalVarTypeInferenceState;
import org.mbte.groovypp.compiler.transformers.CastExpressionTransformer;
import org.mbte.groovypp.compiler.transformers.ExprTransformer;
import static org.mbte.groovypp.compiler.transformers.ExprTransformer.transformExpression;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.LinkedList;

public abstract class CompilerTransformer extends ReturnsAdder implements Opcodes, LocalVarTypeInferenceState {

    public final CompilerStack compileStack;
    public final ClassNode classNode;
    protected final MethodVisitor mv;
    public final int debug;
    public final TypePolicy policy;
    private static final ClassNode USE = ClassHelper.make(Use.class);
    private int nestedLevel;
    LinkedList<CompiledClosureBytecodeExpr> pendingClosures = new LinkedList<CompiledClosureBytecodeExpr> ();
    private int nextClosureIndex = 1;
    private final String baseClosureName;

    public CompilerTransformer(SourceUnit source, ClassNode classNode, MethodNode methodNode, MethodVisitor mv, CompilerStack compileStack, int debug, TypePolicy policy, String baseClosureName) {
        super(source, methodNode);
        this.classNode = classNode;
        this.mv = mv;
        this.debug = debug;
        this.policy = policy;
        this.baseClosureName = baseClosureName;
        this.compileStack = new CompilerStack(compileStack);
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
            if (nestedLevel == 1) {
                if (!pendingClosures.isEmpty()) {
                    for (CompiledClosureBytecodeExpr pendingClosure : pendingClosures) {
                        ClosureClassNode type = (ClosureClassNode) pendingClosure.getType();
                        ClosureMethodNode doCallMethod = type.getDoCallMethod();
                        Statement code = doCallMethod.getCode();
                        if (!(code instanceof BytecodeSequence)) {
                            ClosureUtil.improveClosureType(type, ClassHelper.CLOSURE_TYPE);
                            StaticMethodBytecode.replaceMethodCode(su, doCallMethod, compileStack, debug == -1 ? -1 : debug+1, policy, type.getName());
                        }
                    }
                    pendingClosures.clear();
                }
            }
            return result;
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

    public BytecodeExpr transformLogical(Expression exp, Label label, boolean onTrue) {
        try {
            return ExprTransformer.transformLogicalExpression(exp, this, label, onTrue);
        }
        catch (Throwable e) {
            e.printStackTrace();
            addError(e.getMessage(), exp);
            return null;
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
                    smce.getMethod(),
                    smce.getArguments());
            mce.setSourcePosition(smce);
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
                if (parameters.length > 0 && objectType.isDerivedFrom(parameters[0].getType())) {
                    candidates = createDGM(mn);
                }
            }
        } else {
            FastArray ms = (FastArray) o;
            if (ms == null) return candidates;
            for (int i = 0; i != ms.size(); ++i) {
                MethodNode mn = (MethodNode) ms.get(i);
                if (mn.isStatic()) {
                    final Parameter[] parameters = mn.getParameters();
                    if (parameters.length > 0 && objectType.isDerivedFrom(parameters[0].getType())) {
                        if (candidates == null)
                            candidates = createDGM(mn);
                        else if (candidates instanceof FastArray) {
                            ((FastArray) candidates).add(createDGM(mn));
                        } else {
                            MethodNode _1st = (MethodNode) candidates;
                            candidates = new FastArray(2);
                            ((FastArray) candidates).add(_1st);
                            ((FastArray) candidates).add(createDGM(mn));
                        }
                    }
                }
            }
        }

        return candidates;
    }

    public MethodNode findMethod(ClassNode type, String methodName, ClassNode[] args) {
        Object methods = ClassNodeCache.getMethods(type, methodName);
        final Object res = MethodSelection.chooseMethod(methodName, methods, type, args);
        if (res instanceof MethodNode)
            return (MethodNode) res;

        Object candidates = findCategoryMethod(classNode, methodName, type, args, null);
        final List<AnnotationNode> list = classNode.getAnnotations(USE);
        for (AnnotationNode annotationNode : list) {
            final Expression member = annotationNode.getMember("value");
            if (member != null && (member instanceof ClassExpression)) {
                ClassExpression expression = (ClassExpression) member;
                final ClassNode category = expression.getType();
                candidates = findCategoryMethod(category, methodName, type, args, candidates);
            }
        }

        if (candidates == null) {
            final CompileUnit compileUnit = classNode.getCompileUnit();
            for (ModuleNode moduleNode : compileUnit.getModules()) {
                for (ClassNode category : moduleNode.getClasses()) {
                    candidates = findCategoryMethod(category, methodName, type, args, candidates);
                }
            }
        }

        if (candidates != null) {
            final Object r = MethodSelection.chooseMethod(methodName, candidates, type, args);
            if (r instanceof MethodNode)
                return (MethodNode) r;
        }

        return null;
    }

    private static ClassNodeCache.DGM createDGM(MethodNode method) {
        final Parameter[] pp = method.getParameters();
        Parameter params[] = pp.length > 1 ? new Parameter[pp.length - 1] : Parameter.EMPTY_ARRAY;
        for (int j = 0; j != params.length; ++j)
            params[j] = new Parameter(pp[j + 1].getType(), "$" + j);

        ClassNodeCache.DGM mn = new ClassNodeCache.DGM(
                method.getName(),
                Opcodes.ACC_PUBLIC,
                method.getReturnType(),
                params,
                method.getExceptions(),
                null);
        mn.setDeclaringClass(pp[0].getType());
        mn.callClassInternalName = BytecodeHelper.getClassInternalName(method.getDeclaringClass());
        mn.descr = BytecodeHelper.getMethodDescriptor(method.getReturnType(), method.getParameters());
        mn.setGenericsTypes(method.getGenericsTypes());
        mn.original = method;
        return mn;
    }

    public PropertyNode findProperty(ClassNode type, String property) {
        for (; type != null; type = type.getSuperClass()) {
            PropertyNode propertyNode = type.getProperty(property);
            if (propertyNode != null)
                return propertyNode;
        }
        return null;
    }

    public MethodNode findConstructor(ClassNode type, ClassNode[] args) {
        FastArray methods = ClassNodeCache.getConstructors(type);

        if (type.redirect() instanceof InnerClassNode && (type.getModifiers() & ACC_STATIC) == 0) {
            ClassNode newArgs [] = new ClassNode[args.length+1];
            newArgs [0] = classNode;
            System.arraycopy(args, 0, newArgs, 1, args.length);
            args = newArgs;
        }

        final Object res = MethodSelection.chooseMethod("<init>", methods, type, args);
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

    public BytecodeExpr cast(final BytecodeExpr be, final ClassNode type) {
        if (TypeUtil.isDirectlyAssignableFrom(type, be.getType()))
            return be;

        if (type.equals(ClassHelper.boolean_TYPE) || type.equals(ClassHelper.Boolean_TYPE)) {
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
                    addError("asBoolean should return 'boolean'", be);

                return new BytecodeExpr(be, ClassHelper.boolean_TYPE) {
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

        if (TypeUtil.isAssignableFrom(type, be.getType())) {
            return new CastExpressionTransformer.Cast(type, be);
        }

        if (be.getType().implementsInterface(TypeUtil.TCLOSURE)) {
            List<MethodNode> one = ClosureUtil.isOneMethodAbstract(type);
            MethodNode doCall = one == null ? null : ClosureUtil.isMatch(one, (ClosureClassNode) be.getType(), this, type);
            ClosureUtil.makeOneMethodClass(be.getType(), type, one, doCall);
            return new CastExpressionTransformer.Cast(type, be);
        }

        addError("Can not convert " + be.getType().getName() + " to " + type.getName(), be);
        return null;
    }

    public boolean samePackage(FieldNode fieldNode) {
        PackageNode accessPackage = classNode.getPackage();
        PackageNode fieldPackage = fieldNode.getDeclaringClass().getPackage();
        return (accessPackage == null && fieldPackage == null) || (accessPackage != null && accessPackage.getName().equals(fieldPackage.getName()));
    }

    public ClassNode getCollectionType(ClassNode type) {
        MethodNode methodNode = findMethod(TypeUtil.COLLECTION_TYPE, "add", new ClassNode[]{ClassHelper.OBJECT_TYPE});
        ClassNode paramType = methodNode.getParameters()[0].getType();
        return TypeUtil.getSubstitutedType(paramType, methodNode.getDeclaringClass(), type);
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
