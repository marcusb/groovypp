package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.classgen.BytecodeHelper;

public class ResolvedFieldBytecodeExpr extends BytecodeExpr {
    private final FieldNode fieldNode;
    private final BytecodeExpr object;
    private final BytecodeExpr value;

    public ResolvedFieldBytecodeExpr(Expression parent, FieldNode fieldNode, BytecodeExpr object, BytecodeExpr value) {
        super (parent, fieldNode.getType());
        this.fieldNode = fieldNode;
        this.object = object;
        this.value = value;
    }

    public void compile() {
        int op;
        if (object != null) {
            object.visit(mv);
            if (fieldNode.isStatic()) {
                pop(object.getType());
            }
            else {
                object.box(object.getType());
            }
        }

        if (value == null) {
            op = fieldNode.isStatic() ? GETSTATIC : GETFIELD;
        }
        else {
            op = fieldNode.isStatic() ? PUTSTATIC : PUTFIELD;
            value.visit(mv);

            box (value.getType());
            cast(ClassHelper.getWrapper(value.getType()), ClassHelper.getWrapper(fieldNode.getType()));
            unbox(fieldNode.getType());
        }
        mv.visitFieldInsn(op, BytecodeHelper.getClassInternalName(fieldNode.getDeclaringClass()), fieldNode.getName(), BytecodeHelper.getTypeDescription(fieldNode.getType()));
    }
}