package org.mbte.groovypp.compiler

import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.syntax.SyntaxException
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.mbte.groovypp.compiler.TypeUtil
import org.objectweb.asm.Opcodes
import org.codehaus.groovy.ast.*
import static org.codehaus.groovy.ast.ClassHelper.make
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.*

@Typed
@GroovyASTTransformation (phase = CompilePhase.CONVERSION)
class GrunitASTTransform implements ASTTransformation, Opcodes {
    static final ClassNode GROOVY_TEST_CASE = make(GroovyTestCase)

    void visit(ASTNode[] nodes, SourceUnit source) {
        if (!source.name.endsWith(".grunit"))
            return

        ExpressionTransformer transformer = { exp -> exp.transformExpression(this) }
        
        ModuleNode module = nodes[0]
        def classes = module.classes
        if (classes) {
            if (!classes[0].script) {
                source.addError(new SyntaxException("Can't find body of .grunit script", null, -1, -1))
                return
            }

            def classNode = classes[0]
            cleanScriptMethods (classNode)

            def accumulated = new BlockStatement()

            for(Iterator<Statement> it = module.statementBlock.statements.iterator(); it.hasNext(); ) {
                def statement = it.next()
                if (statement instanceof ExpressionStatement) {
                    def expr = ((ExpressionStatement)statement).expression
                    if (expr instanceof DeclarationExpression) {
                        DeclarationExpression decl = expr
                        VariableExpression ve = decl.leftExpression
                        if (hasFieldAnnotation(ve)) {
                            classNode.addField(ve.name, Opcodes.ACC_PRIVATE, ve.type, ConstantExpression.NULL)
                            it.remove()
                            continue
                        }
                    } else
                    if (expr instanceof MethodCallExpression) {
                        MethodCallExpression methodCall = expr
                        if (methodCall.objectExpression == VariableExpression.THIS_EXPRESSION) {
                            def method = methodCall.method
                            if (method instanceof ConstantExpression) {
                                def methodName = ((ConstantExpression)method).text
                                switch(methodName) {
                                    case "extendsTest":
                                        extendsTest (statement, methodCall, classes[0], source)
                                        it.remove()
                                        continue
                                    break

                                    case "setUp":
                                        setUp (statement, methodCall, classes[0], source)
                                        it.remove()
                                        continue
                                    break

                                    case "tearDown":
                                        tearDown (statement, methodCall, classes[0], source)
                                        it.remove()
                                        continue
                                    break

                                    default:
                                      if(defaultMethod(statement, methodCall, classes[0], source, accumulated, transformer)) {
                                        it.remove ()
                                        continue
                                      }
                                }
                            }
                        }
                    }
                }

                accumulate( accumulated, statement, transformer)
            }

            if (!module.statementBlock.empty) {
                classes[0].addMethod("test\$main", Opcodes.ACC_PUBLIC, ClassHelper.VOID_TYPE, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, accumulated)
            }
        }
    }

    void extendsTest(Statement statement, MethodCallExpression methodCall, ClassNode classNode, SourceUnit source) {
        if (!methodCall.arguments instanceof ArgumentListExpression) {
            source.addError(new SyntaxException("extendsTest should have exactly one argument: name of extended class", null, -1, -1))
            return
        }
        def args = ((ArgumentListExpression)methodCall.arguments).expressions
        if (!args) {
            source.addError(new SyntaxException("extendsTest should have exactly one argument: name of extended class", null, -1, -1))
            return
        }
        ClassNode classname = ClassHelper.make(args[0].text)
        classNode.setSuperClass(classname)
    }

    void setUp(Statement statement, MethodCallExpression methodCall, ClassNode classNode, SourceUnit source) {
        if (!methodCall.arguments instanceof ArgumentListExpression) {
            source.addError(new SyntaxException("setUp should have exactly one argument: closure without parameters", null, -1, -1))
            return
        }
        def args = ((ArgumentListExpression)methodCall.arguments).expressions
        if (!args || !(args[0] instanceof ClosureExpression) || ((ClosureExpression)args[0]).parameters?.length > 1) {
            source.addError(new SyntaxException("setUp should have exactly one argument: closure without parameters", null, -1, -1))
            return
        }

        def code = new BlockStatement()
        code.addStatement (new ExpressionStatement(
                new MethodCallExpression(
                        VariableExpression.SUPER_EXPRESSION,
                        "setUp",
                        new ArgumentListExpression()
                )
        ))
        code.addStatement(((ClosureExpression)args[0]).code)
        classNode.addMethod("setUp", Opcodes.ACC_PROTECTED, ClassHelper.VOID_TYPE, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, code)
    }

    void tearDown(Statement statement, MethodCallExpression methodCall, ClassNode classNode, SourceUnit source) {

        if (!methodCall.arguments instanceof ArgumentListExpression) {
            source.addError(new SyntaxException("tearDown should have exactly one argument: closure without parameters", null, -1, -1))
            return
        }
        def args = ((ArgumentListExpression)methodCall.arguments).expressions
        if (!args || !(args[0] instanceof ClosureExpression) || ((ClosureExpression)args[0]).parameters?.length > 1) {
            source.addError(new SyntaxException("tearDown should have exactly one argument: closure without parameters", null, -1, -1))
            return
        }

        def code = new BlockStatement()
        code.addStatement(((ClosureExpression)args[0]).code)
        code.addStatement (new ExpressionStatement(
                new MethodCallExpression(
                        VariableExpression.SUPER_EXPRESSION,
                        "tearDown",
                        new ArgumentListExpression()
                )
        ))
        classNode.addMethod("tearDown", Opcodes.ACC_PROTECTED, ClassHelper.VOID_TYPE, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, code)
    }

    boolean defaultMethod(Statement statement, MethodCallExpression methodCall, ClassNode classNode, SourceUnit source, BlockStatement accumulated, ExpressionTransformer transformer) {
        if(methodCall.methodAsString.startsWith("test")) {
            if (methodCall.arguments instanceof ArgumentListExpression) {
                def args = ((ArgumentListExpression)methodCall.arguments).expressions
                if (args && (args[0] instanceof ClosureExpression) && ((ClosureExpression)args[0]).parameters?.length <= 1) {
                    BlockStatement soFar = cloneStatement(accumulated, transformer)
                    accumulate(soFar, ((ClosureExpression)args[0]).code, transformer)
                    classNode.addMethod(methodCall.methodAsString, Opcodes.ACC_PUBLIC, ClassHelper.VOID_TYPE, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, soFar)
                    return true
                }
            }
        }

        if(methodCall.methodAsString.startsWith("check")) {
            if (methodCall.arguments instanceof ArgumentListExpression) {
                def args = ((ArgumentListExpression)methodCall.arguments).expressions
                if (args && (args[0] instanceof ClosureExpression) && ((ClosureExpression)args[0]).parameters?.length <= 1) {
                    accumulate( accumulated, ((ClosureExpression)args[0]).code, transformer)
                    classNode.addMethod("test" + methodCall.methodAsString.substring(5), Opcodes.ACC_PUBLIC, ClassHelper.VOID_TYPE, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, cloneStatement(accumulated, transformer))
                    return true
                }
            }
        }

        return false
    }

    void cleanScriptMethods(ClassNode classNode) {
        classNode.superClass = ClassHelper.make("groovy.util.GroovyTestCase")
        classNode.declaredConstructors.clear()

        for(def it = classNode.methods.iterator(); it.hasNext(); ) {
            def mn = it.next()
            if (mn.name.equals("run")) {
                it.remove()
                continue
            }

            if (mn.name.equals("main")) {
                mn.setCode(
                        new ExpressionStatement(
                                new MethodCallExpression(
                                        new ClassExpression(ClassHelper.make("junit.textui.TestRunner")),
                                        "run",
                                        new ArgumentListExpression(
                                                new ClassExpression(
                                                        classNode
                                                )
                                        )
                                )
                        )
                )
            }
        }
    }

    private boolean hasFieldAnnotation(VariableExpression ve) {
        for (AnnotationNode node : ve.getAnnotations()) {
            if ("Field".equals(node.getClassNode().getName()))
                return true;
            if ("groovy.lang.Field".equals(node.getClassNode().getName()))
                return true;
        }
        return false;
    }
    
    private Statement cloneStatement(Statement statement, ExpressionTransformer transformer) {
        switch(statement) {
            case AssertStatement:
                AssertStatement src = statement
                def add = new AssertStatement(new BooleanExpression(transformer.transform(src.booleanExpression.expression)), transformer.transform(src.messageExpression))
                add.setSourcePosition(src)
                add
                break

            case BlockStatement:
                BlockStatement src = statement
                def add = new BlockStatement()
                for (s in src.statements)
                   add.statements.add(cloneStatement(s, transformer))
                add.setSourcePosition(src)
                add
                break

            case EmptyStatement:
            case BreakStatement:
            case ContinueStatement:
                statement
                break

            case CaseStatement:
                CaseStatement src = statement
                def add = new CaseStatement(transformer.transform(src.expression), cloneStatement(src.code, transformer))
                add.setSourcePosition(src)
                add
                break

            case CatchStatement:
                CatchStatement src = statement
                def add = new CatchStatement([src.variable.type, src.variable.name], cloneStatement(src.code, transformer))
                add.setSourcePosition(src)
                add
                break


            case ExpressionStatement:
                ExpressionStatement src = statement
                def add = new ExpressionStatement(transformer.transform(src.expression))
                add.setSourcePosition(src)
                add
                break

            case ForStatement:
                ForStatement src = statement
                def add = new ForStatement([src.variable.type, src.variable.name], transformer.transform(src.collectionExpression), cloneStatement(src.loopBlock, transformer))
                add.setSourcePosition(src)
                add
                break

            case IfStatement:
                IfStatement src = statement
                def add = new IfStatement(new BooleanExpression(transformer.transform(src.booleanExpression.expression)), cloneStatement(src.ifBlock, transformer), cloneStatement(src.elseBlock, transformer))
                add.setSourcePosition(src)
                add
                break

            case ReturnStatement:
                ReturnStatement src = statement
                def add = new ReturnStatement(transformer.transform(src.expression))
                add.setSourcePosition(src)
                add
                break

            case SwitchStatement:
                SwitchStatement src = statement
                def add = new SwitchStatement(transformer.transform(src.expression), cloneStatement(src.defaultStatement, transformer))
                for (c in src.caseStatements)
                    add.addCase((CaseStatement)cloneStatement(c, transformer))
                add.setSourcePosition(src)
                add
                break

            case SynchronizedStatement:
                SynchronizedStatement src = statement
                def add = new SynchronizedStatement(transformer.transform(src.expression), cloneStatement(src.code, transformer))
                add.setSourcePosition(src)
                add
                break

            case ThrowStatement:
                ThrowStatement src = statement
                def add = new ThrowStatement(src.expression)
                add.setSourcePosition(src)
                add
                break

            case TryCatchStatement:
                TryCatchStatement src = statement
                def add = new TryCatchStatement(cloneStatement(src.tryStatement, transformer), cloneStatement(src.finallyStatement, transformer))
                for (c in src.catchStatements)
                    add.addCatch((CatchStatement)cloneStatement(c, transformer))
                add.setSourcePosition(src)
                add
                break

            case DoWhileStatement:
                DoWhileStatement src = statement
                def add = new WhileStatement(new BooleanExpression(transformer.transform(src.booleanExpression.expression)), cloneStatement(src.loopBlock, transformer))
                add.setSourcePosition(src)
                add
                break

            case WhileStatement:
                WhileStatement src = statement
                def add = new WhileStatement(new BooleanExpression(transformer.transform(src.booleanExpression.expression)), cloneStatement(src.loopBlock, transformer))
                add.setSourcePosition(src)
                add
                break
        }
    }

    void accumulate(BlockStatement accumulator, Statement statement, ExpressionTransformer transformer) {
        if (statement instanceof BlockStatement) {
            for (s in ((BlockStatement)statement).statements)
                accumulator.statements.add(cloneStatement(s, transformer))
        }
        else {
            accumulator.statements.add(cloneStatement(statement, transformer))
        }
    }
}
