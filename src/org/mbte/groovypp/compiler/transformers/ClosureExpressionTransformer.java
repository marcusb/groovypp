package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.*;
import org.mbte.groovypp.compiler.*;
import org.mbte.groovypp.compiler.bytecode.PropertyUtil;
import org.objectweb.asm.Opcodes;

import java.util.Iterator;

public class ClosureExpressionTransformer extends ExprTransformer<ClosureExpression> {
    public Expression transform(ClosureExpression ce, CompilerTransformer compiler) {

        if (ce.getParameters() != null && ce.getParameters().length == 0) {
            final VariableScope scope = ce.getVariableScope();
            ce = new ClosureExpression(new Parameter[1], ce.getCode());
            ce.setVariableScope(scope);
            ce.getParameters()[0] = new Parameter(ClassHelper.OBJECT_TYPE, "it", new ConstantExpression(null));
        }

        final ClosureClassNode newType = new ClosureClassNode(compiler.methodNode, compiler.getNextClosureName());
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

        if (!compiler.methodNode.isStatic() || compiler.classNode.getName().endsWith("$TraitImpl")) {
            if (usesOuterInstance(ce))
                newType.addField("this$0", ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, !compiler.methodNode.isStatic() ? compiler.classNode : compiler.methodNode.getParameters()[0].getType(), null);
        }

        _doCallMethod.createDependentMethods(newType);

        newType.setModule(compiler.classNode.getModule());
        ce.setType(newType);

        return CompiledClosureBytecodeExpr.createCompiledClosureBytecodeExpr(compiler, ce);
    }

    private static boolean usesOuterInstance(ClosureExpression ce) {
        Iterator<Variable> it = ce.getVariableScope().getReferencedClassVariablesIterator();
        while (it.hasNext()) {
            Variable variable = it.next();
            if (variable instanceof DynamicVariable || // the variable might be static, but we don't have a way to tell.
                !PropertyUtil.isStatic(variable)) return true;
        }
        return false;
    }
}
