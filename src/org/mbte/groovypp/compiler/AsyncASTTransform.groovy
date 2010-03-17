package org.mbte.groovypp.compiler

import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.ast.*
import static org.codehaus.groovy.ast.ClassHelper.make
import org.objectweb.asm.Opcodes

import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.ast.expr.CastExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.TernaryExpression
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.classgen.VariableScopeVisitor

@Typed
@GroovyASTTransformation (phase = CompilePhase.CONVERSION)
class AsyncASTTransform implements ASTTransformation, Opcodes {

    static final boolean RESTRICTED

    static final ClassNode CALL_LATER  = ClassHelper.makeWithoutCaching("CallLater")
    static final ClassNode BIND_LATER  = ClassHelper.makeWithoutCaching("BindLater")
    static final ClassNode LISTENER    = ClassHelper.makeWithoutCaching("BindLater.Listener")
    static final ClassNode EXECUTOR    = ClassHelper.makeWithoutCaching("Executor")

    void visit(ASTNode[] nodes, SourceUnit source) {
        if (RESTRICTED)
            return

        List<MethodNode> toProcess = []

        ModuleNode module = (ModuleNode) nodes[0];
        module.addStarImport("groovy.util.concurrent")
        module.addStarImport("java.util.concurrent")
        for (ClassNode classNode: module.getClasses()) {
            for (MethodNode methodNode: classNode.getMethods()) {
                for (AnnotationNode ann : methodNode.getAnnotations()) {
                    if (ann.getClassNode().getNameWithoutPackage().equals("Async")) {
                        toProcess << methodNode
                    }
                }
            }
        }

        for (methodNode in toProcess)
        {
            final ClassNode ret = ClassHelper.getWrapper(methodNode.getReturnType())

            def args = new ArgumentListExpression();

            def origParams = methodNode.parameters
            def params = new Parameter[origParams.length + 2]
            for (i in 0..<origParams.length) {
                def name = "p\$" + i
                params[i] = [origParams[i].type, name]
                args.addExpression(new VariableExpression(params[i]))
            }
            params[-2] = [EXECUTOR, "executor"]
            params[-1] = [TypeUtil.withGenericTypes(LISTENER, ret), "listener", ConstantExpression.NULL]

            def code = new BlockStatement()
            code.setVariableScope(new VariableScope())

            def innerCode = new BlockStatement()
            innerCode.setVariableScope(new VariableScope(code.getVariableScope()))
            innerCode.addStatement(
                    new ExpressionStatement(
                            new MethodCallExpression(
                                    VariableExpression.THIS_EXPRESSION,
                                    methodNode.getName(),
                                    args
                            )
                    )
            )

            def futureDecl = new DeclarationExpression(
                    new VariableExpression("future"),
                    Token.newSymbol(Types.EQUAL, -1, -1),
                    new CastExpression(
                            CALL_LATER,
                            new ClosureExpression(null, innerCode)
                    )
            )
            code.addStatement(new ExpressionStatement(futureDecl))
            code.addStatement(
                    new ExpressionStatement(
                            new MethodCallExpression(
                                    new VariableExpression(params[-2]),
                                    "execute",
                                    new ArgumentListExpression(
                                            new TernaryExpression(
                                                    new BooleanExpression(new VariableExpression(params[-1])),
                                                    new CastExpression(
                                                            CALL_LATER,
                                                            new MethodCallExpression(
                                                                    new VariableExpression("future"),
                                                                    "whenBound",
                                                                    new ArgumentListExpression(new VariableExpression(params[-1]))
                                                            )
                                                    ),
                                                    new VariableExpression("future")
                                            )
                                    )
                            )
                    )
            )
            code.addStatement(new ReturnStatement(new VariableExpression("future")))

            methodNode.getDeclaringClass().addMethod(
                    methodNode.getName(),
                    ACC_PUBLIC,
                    TypeUtil.withGenericTypes(BIND_LATER, ret),
                    params,
                    ClassNode.EMPTY_ARRAY,
                    code
            )
        }
    }
}
