package org.mbte.groovypp.compiler;

import groovy.lang.CompilePolicy;
import groovy.lang.Use;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.util.FastArray;
import org.codehaus.groovy.reflection.CachedMethod;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.bytecode.LocalVarTypeInferenceState;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.transformers.ExprTransformer;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Label;

import java.util.List;

public abstract class CompilerTransformer extends ReturnsAdder implements Opcodes, LocalVarTypeInferenceState {

    public final CompilerStack compileStack;
    public final ClassNode classNode;
    protected final MethodVisitor mv;
    public final CompilePolicy policy;
    private static final ClassNode USE = ClassHelper.make(Use.class);

    public CompilerTransformer(SourceUnit source, ClassNode classNode, MethodNode methodNode, MethodVisitor mv, CompilerStack compileStack, CompilePolicy policy) {
        super(source, methodNode);
        this.classNode = classNode;
        this.mv = mv;
        this.policy = policy;
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
        try {
            return ExprTransformer.transformExpression(exp, this);
        }
        catch (Throwable e) {
            e.printStackTrace();
            addError(e.getMessage(), exp);
            return null;
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

    public MethodNode findMethod(ClassNode type, String methodName, ClassNode [] args) {
        Object methods = ClassNodeCache.getMethods(type, methodName);
        final Object res = MethodSelection.chooseMethod(methodName, methods, args);
        if (res instanceof MethodNode)
            return (MethodNode)res;

        Object candidates = null;
        final List<AnnotationNode> list = classNode.getAnnotations(USE);
        for (AnnotationNode annotationNode : list) {
            final Expression member = annotationNode.getMember("value");
            if (member != null && (member instanceof ClassExpression)) {
                ClassExpression expression = (ClassExpression) member;
                final ClassNode category = expression.getType();

                final Object o = ClassNodeCache.getMethods(category, methodName);
                if (o instanceof MethodNode) {
                    MethodNode mn = (MethodNode) o;
                    if (mn.isStatic()) {
                        final Parameter[] parameters = mn.getParameters();
                        if (parameters.length > 0 && type.isDerivedFrom(parameters[0].getType())) {
                            candidates = createDGM(mn);
                        }
                    }
                }
                else {
                    FastArray ms = (FastArray) o;
                    if (ms==null) return null;
                    for (int i = 0; i != ms.size(); ++i) {
                        MethodNode mn = (MethodNode) ms.get(i);
                        if (mn.isStatic()) {
                            final Parameter[] parameters = mn.getParameters();
                            if (parameters.length > 0 && type.isDerivedFrom(parameters[0].getType())) {
                                if (candidates == null)
                                    candidates = createDGM(mn);
                                else
                                    if (candidates instanceof FastArray) {
                                        ((FastArray)candidates).add(createDGM(mn));
                                    }
                                    else {
                                        MethodNode _1st = (MethodNode)candidates;
                                        candidates = new FastArray(2);
                                        ((FastArray)candidates).add(_1st);
                                        ((FastArray)candidates).add(createDGM(mn));
                                    }
                            }
                        }
                    }
                }
            }
        }

        if (candidates != null) {
            final Object r = MethodSelection.chooseMethod(methodName, candidates, args);
            if (r instanceof MethodNode)
                return (MethodNode)r;
        }

        return null;
    }

    private static ClassNodeCache.DGM createDGM(MethodNode method) {
        final Parameter[] pp = method.getParameters();
        Parameter params [] = pp.length > 1 ? new Parameter[pp.length-1] : Parameter.EMPTY_ARRAY;
        for (int j = 0; j != params.length; ++j)
          params[j] = new Parameter(pp[j+1].getType(), "$"+j);

        ClassNodeCache.DGM mn = new ClassNodeCache.DGM(
                method.getName(),
                Opcodes.ACC_PUBLIC,
                method.getReturnType(),
                params,
                method.getExceptions(),
                null);
        mn.setDeclaringClass(pp[0].getType());
        mn.callClassInternalName = BytecodeHelper.getClassInternalName(method.getDeclaringClass());
        mn.descr = BytecodeHelper.getMethodDescriptor(method.getReturnType(),method.getParameters());
        return mn;
    }

    public PropertyNode findProperty(ClassNode type, String property) {
        for (;type != null; type = type.getSuperClass()) {
            PropertyNode propertyNode = type.getProperty(property);
            if (propertyNode != null)
                return propertyNode;
        }
        return null;
    }

    public MethodNode findConstructor(ClassNode type, ClassNode[] args) {
        FastArray methods = ClassNodeCache.getConstructors(type) ;

        final Object res = MethodSelection.chooseMethod("<init>", methods, args);
        if (res instanceof MethodNode)
            return (MethodNode)res;
        return null;
    }

    public ClassNode[] exprToTypeArray(Expression args) {
        final List list = ((TupleExpression) args).getExpressions();
        final ClassNode[] nodes = new ClassNode[list.size()];
        for (int i = 0; i < nodes.length; i++) {
            ClassNode type = ((Expression) list.get(i)).getType();
            if (type == TypeUtil.NULL_TYPE)
                nodes [i] = null;
            else
                nodes [i] = type;
        }
        return nodes;
    }

    public void mathOp(ClassNode type, Token op, BinaryExpression be) {
        switch (op.getType()) {
            case Types.PLUS:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(IADD);
                else
                if (type == ClassHelper.double_TYPE)
                    mv.visitInsn(DADD);
                else
                if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LADD);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.COMPARE_NOT_EQUAL: {
                Label _true = new Label();
                if (type == ClassHelper.int_TYPE)
                    mv.visitJumpInsn(IF_ICMPEQ, _true);
                else
                if (type == ClassHelper.double_TYPE) {
                    mv.visitInsn(DCMPG);
                    mv.visitJumpInsn(IFEQ, _true);
                }
                else
                if (type == ClassHelper.float_TYPE) {
                    mv.visitInsn(FCMPG);
                    mv.visitJumpInsn(IFEQ, _true);
                }
                else
                if (type == ClassHelper.long_TYPE) {
                    mv.visitInsn(LCMP);
                    mv.visitJumpInsn(IFEQ, _true);
                }
                else
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
                else
                if (type == ClassHelper.double_TYPE)
                    mv.visitInsn(DMUL);
                else
                if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LMUL);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.MINUS:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(ISUB);
                else
                if (type == ClassHelper.double_TYPE)
                    mv.visitInsn(DSUB);
                else
                if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LSUB);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.DIVIDE:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(IDIV);
                else
                if (type == ClassHelper.double_TYPE)
                    mv.visitInsn(DDIV);
                else
                if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LDIV);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.BITWISE_XOR:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(IXOR);
                else
                if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LXOR);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.BITWISE_AND:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(IAND);
                else
                if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LAND);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.INTDIV:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(IDIV);
                else
                if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LDIV);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.LEFT_SHIFT:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(ISHL);
                else
                if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LSHL);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.RIGHT_SHIFT:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(ISHR);
                else
                if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LSHR);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.RIGHT_SHIFT_UNSIGNED:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(IUSHR);
                else
                if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LUSHR);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.MOD:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(IREM);
                else
                if (type == ClassHelper.double_TYPE)
                    mv.visitInsn(DREM);
                else
                if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LREM);
                else
                    throw new RuntimeException("Internal Error");
                break;

            case Types.BITWISE_OR:
                if (type == ClassHelper.int_TYPE)
                    mv.visitInsn(IOR);
                else
                if (type == ClassHelper.long_TYPE)
                    mv.visitInsn(LOR);
                else
                    throw new RuntimeException("Internal Error");
                break;


            default:
                addError("Operation " + op.getDescription() + " doesn't supported", be);
        }
    }
}
