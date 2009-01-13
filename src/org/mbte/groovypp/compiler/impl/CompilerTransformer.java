package org.mbte.groovypp.compiler.impl;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.classgen.*;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.syntax.Token;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.mbte.groovypp.compiler.impl.expressions.ExprTransformer;

import java.util.*;

public abstract class CompilerTransformer extends ReturnsAdder implements Opcodes, LocalVarTypeInferenceState  {

    public final CompilerStack compileStack;
    public final ClassNode classNode;
    protected final MethodVisitor mv;

    public CompilerTransformer(SourceUnit source, ClassNode classNode, MethodNode methodNode, MethodVisitor mv, CompilerStack compileStack) {
        super(source, methodNode);
        this.classNode = classNode;
        this.mv = mv;
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

        return null;
    }

    public ClassNode[] exprToTypeArray(Expression args) {
        final List list = ((ArgumentListExpression) args).getExpressions();
        final ClassNode[] nodes = new ClassNode[list.size()];
        for (int i = 0; i < nodes.length; i++) {
            nodes [i] = ((Expression)list.get(i)).getType();
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
                if (type == ClassHelper.BigDecimal_TYPE)
                    mv.visitMethodInsn(INVOKEVIRTUAL, BytecodeHelper.getClassInternalName(ClassHelper.BigDecimal_TYPE), "add", "(Ljava/math/BigDecimal;)Ljava/math/BigDecimal;");
                else
                if (type == ClassHelper.BigInteger_TYPE)
                    mv.visitMethodInsn(INVOKEVIRTUAL, BytecodeHelper.getClassInternalName(ClassHelper.BigInteger_TYPE), "add", "(Ljava/math/BigInteger;)Ljava/math/BigInteger;");
                else
                    throw new RuntimeException("Internal Error");
                break;

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

            default:
                addError("Operation " + op.getDescription() + " doesn't supported", be);
        }
    }
}
