package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.*;
import org.mbte.groovypp.compiler.*;
import org.objectweb.asm.Opcodes;

public class ClosureExpressionTransformer extends ExprTransformer<ClosureExpression> {
    public Expression transform(ClosureExpression ce, CompilerTransformer compiler) {

        boolean addDefault = false;
        if (ce.getParameters() != null && ce.getParameters().length == 0) {
            addDefault = true;
            final VariableScope scope = ce.getVariableScope();
            ce = new ClosureExpression(new Parameter[1], ce.getCode());
            ce.setVariableScope(scope);
            ce.getParameters()[0] = new Parameter(ClassHelper.OBJECT_TYPE, "it");
        }

        final ClosureClassNode newType = new ClosureClassNode(compiler.classNode, compiler.getNextClosureName());
        newType.setInterfaces(new ClassNode[]{TypeUtil.TCLOSURE});
        newType.setClosureExpression(ce);

        final ClosureMethodNode _doCallMethod = new ClosureMethodNode(
                "doCall",
                Opcodes.ACC_PUBLIC,
                ClassHelper.OBJECT_TYPE,
                ce.getParameters() == null ? Parameter.EMPTY_ARRAY : ce.getParameters(),
                ce.getCode());

        newType.addMethod(_doCallMethod);
        newType.setDoCallMethod(_doCallMethod);

        if (!compiler.methodNode.isStatic() || compiler.classNode.getName().endsWith("$TraitImpl"))
            newType.addField("this$0", Opcodes.ACC_PUBLIC, !compiler.methodNode.isStatic() ? compiler.classNode : compiler.methodNode.getParameters()[0].getType(), null);

        if (addDefault) {
            ClosureUtil.createDependentMethod(newType, _doCallMethod);
        }


        newType.setModule(compiler.classNode.getModule());
        ce.setType(newType);

        return CompiledClosureBytecodeExpr.createCompiledClosureBytecodeExpr(compiler, ce);
    }
}
