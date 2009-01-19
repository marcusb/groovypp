package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.Verifier;
import org.mbte.groovypp.compiler.ClassNodeCache;
import org.mbte.groovypp.compiler.CompiledClosureBytecodeExpr;
import org.mbte.groovypp.compiler.TypeUtil;

public class ResolvedPropertyBytecodeExpr extends BytecodeExpr {
    private final PropertyNode propertyNode;
    private final BytecodeExpr object;
    private final String methodName;
    private final BytecodeExpr bargs;

    public ResolvedPropertyBytecodeExpr(Expression parent, PropertyNode propertyNode, BytecodeExpr object, BytecodeExpr bargs) {
        super (parent, propertyNode.getType());
        this.propertyNode = propertyNode;
        this.object = object;
        this.bargs = bargs;

        if (bargs != null) {
            methodName = "set" + Verifier.capitalize(propertyNode.getName());
        }
        else {
            methodName = "get" + Verifier.capitalize(propertyNode.getName());
        }
    }

    public void compile() {
        final String classInternalName;
        final String methodDescriptor;

        int op = INVOKEVIRTUAL;
        if (propertyNode.isStatic())
          op = INVOKESTATIC;

        if (object != null) {
            object.visit(mv);
            box(object.getType());
        }

        if (op == INVOKESTATIC && object != null) {
            if (ClassHelper.long_TYPE == object.getType() || ClassHelper.double_TYPE == object.getType())
                mv.visitInsn(POP2);
            else
                mv.visitInsn(POP);
        }
        classInternalName = BytecodeHelper.getClassInternalName(propertyNode.getDeclaringClass());

        if (methodName.startsWith("set")) {
            bargs.visit(mv);
            final ClassNode paramType = propertyNode.getType();
            final ClassNode type = bargs.getType();
            box(type);
            bargs.cast(ClassHelper.getWrapper(type), ClassHelper.getWrapper(paramType));
            bargs.unbox(paramType);

            methodDescriptor = BytecodeHelper.getMethodDescriptor(propertyNode.getType(), new Parameter[]{new Parameter(paramType, "")});
            mv.visitMethodInsn(op, classInternalName, methodName, methodDescriptor);
        }
        else {
            methodDescriptor = BytecodeHelper.getMethodDescriptor(propertyNode.getType(), Parameter.EMPTY_ARRAY);
            mv.visitMethodInsn(op, classInternalName, methodName, methodDescriptor);
        }
    }
}