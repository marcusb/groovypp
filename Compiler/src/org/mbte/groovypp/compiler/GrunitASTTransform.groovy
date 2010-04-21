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
        ModuleNode module = nodes[0]
        if (source.name.endsWith(".grunit")) {
            def classes = module.classes
            if (classes) {
                if (!classes[0].script) {
                    source.addError(new SyntaxException("Can't find body of .grunit script", null, -1, -1))
                    return
                }

                def classNode = classes[0]

                cleanScriptMethods(classNode)
                processGrunitScript(source, classNode, module.statementBlock, true, "test\$main")
            }
        }
        else {
            List<InnerClassNode> toAdd = []
            for (c in module.classes) {
                if (!(c instanceof InnerClassNode))
                    processClass(c, source, toAdd)
            }

            for(c in toAdd)
                module.addClass(c)
        }
    }

    void processGrunitScript(SourceUnit source, ClassNode classNode, Statement code, boolean classLevel, String defMethodName) {
        ExpressionTransformer transformer = {exp -> exp.transformExpression(this) }

        BlockStatement bsCode
        if(!(code instanceof BlockStatement)) {
            bsCode = new BlockStatement()
            bsCode.statements.add(code)
        }
        else {
            bsCode = code
        }

        def accumulated = new BlockStatement()

        for (Iterator<Statement> it = bsCode.statements.iterator(); it.hasNext();) {
            def statement = it.next()
            if (statement instanceof ExpressionStatement) {
                def expr = ((ExpressionStatement) statement).expression
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
                                def methodName = ((ConstantExpression) method).text
                                switch (methodName) {
                                    case "extendsTest":
                                        if (!classLevel) {
                                            source.addError(new SyntaxException("extendsTest is allowed only on class level annotation", null, -1, -1))
                                            return
                                        }
                                        extendsTest(statement, methodCall, classNode, source)
                                        it.remove()
                                        continue
                                        break

                                    case "setUp":
                                        if (!classLevel) {
                                            source.addError(new SyntaxException("setUp is allowed only on class level annotation", null, -1, -1))
                                            return
                                        }
                                        setUp(statement, methodCall, classNode, source)
                                        it.remove()
                                        continue
                                        break

                                    case "tearDown":
                                        if (!classLevel) {
                                            source.addError(new SyntaxException("tearDown is allowed only on class level annotation", null, -1, -1))
                                            return
                                        }
                                        tearDown(statement, methodCall, classNode, source)
                                        it.remove()
                                        continue
                                        break

                                    default:
                                        if (defaultMethod(statement, methodCall, classNode, source, accumulated, transformer)) {
                                            it.remove()
                                            continue
                                        }
                                }
                            }
                        }
                    }
            }

            accumulate(accumulated, statement, transformer)
        }

        if (!accumulated.empty) {
            classNode.addMethod(findName(defMethodName, classNode), Opcodes.ACC_PUBLIC, ClassHelper.VOID_TYPE, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, accumulated)
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
                    classNode.addMethod(findName(methodCall.methodAsString, classNode), Opcodes.ACC_PUBLIC, ClassHelper.VOID_TYPE, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, soFar)
                    return true
                }
            }
        }

        if(methodCall.methodAsString.startsWith("check")) {
            if (methodCall.arguments instanceof ArgumentListExpression) {
                def args = ((ArgumentListExpression)methodCall.arguments).expressions
                if (args && (args[0] instanceof ClosureExpression) && ((ClosureExpression)args[0]).parameters?.length <= 1) {
                    accumulate( accumulated, ((ClosureExpression)args[0]).code, transformer)
                    classNode.addMethod(findName("test" + methodCall.methodAsString.substring(5), classNode), Opcodes.ACC_PUBLIC, ClassHelper.VOID_TYPE, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, cloneStatement(accumulated, transformer))
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
                return true
            if ("groovy.lang.Field".equals(node.getClassNode().getName()))
                return true
        }
        return false;
    }
    
    private Statement hasGrunitAnnotation(AnnotatedNode ve, SourceUnit source) {
        for (def it = ve.annotations.iterator(); it.hasNext(); ) {
            def node = it.next()
            if ("GrUnit" == node.classNode.name || "groovy.lang.GrUnit" == node.classNode.name) {
                Expression value = node.getMember("value")
                if (!(value instanceof ClosureExpression)) {
                    source.addError(new SyntaxException("Closure expression expected", null, ve.lineNumber, ve.columnNumber))
                    return null
                }
                ClosureExpression closure = value
                if (closure.parameters.length > 1) {
                    source.addError(new SyntaxException("GrUnit closure should have no parameters", null, ve.lineNumber, ve.columnNumber))
                    return null
                }
                it.remove()
                return closure.code
            }
        }
        null
    }

    private Statement cloneStatement(Statement src, ExpressionTransformer transformer) {
        switch(src) {
            case AssertStatement:
                def add = new AssertStatement(new BooleanExpression(transformer.transform(src.booleanExpression.expression)), transformer.transform(src.messageExpression))
                add.setSourcePosition(src)
                add
                break

            case BlockStatement:
                def add = new BlockStatement()
                for (s in src.statements)
                   add.statements.add(cloneStatement(s, transformer))
                add.setSourcePosition(src)
                add
                break

            case EmptyStatement:
            case BreakStatement:
            case ContinueStatement:
                src
                break

            case CaseStatement:
                def add = new CaseStatement(transformer.transform(src.expression), cloneStatement(src.code, transformer))
                add.setSourcePosition(src)
                add
                break

            case CatchStatement:
                def add = new CatchStatement([src.variable.type, src.variable.name], cloneStatement(src.code, transformer))
                add.setSourcePosition(src)
                add
                break


            case ExpressionStatement:
                def add = new ExpressionStatement(transformer.transform(src.expression))
                add.setSourcePosition(src)
                add
                break

            case ForStatement:
                def add = new ForStatement([src.variable.type, src.variable.name], transformer.transform(src.collectionExpression), cloneStatement(src.loopBlock, transformer))
                add.setSourcePosition(src)
                add
                break

            case IfStatement:
                def add = new IfStatement(new BooleanExpression(transformer.transform(src.booleanExpression.expression)), cloneStatement(src.ifBlock, transformer), cloneStatement(src.elseBlock, transformer))
                add.setSourcePosition(src)
                add
                break

            case ReturnStatement:
                def add = new ReturnStatement(transformer.transform(src.expression))
                add.setSourcePosition(src)
                add
                break

            case SwitchStatement:
                def add = new SwitchStatement(transformer.transform(src.expression), cloneStatement(src.defaultStatement, transformer))
                for (c in src.caseStatements)
                    add.addCase((CaseStatement)cloneStatement(c, transformer))
                add.setSourcePosition(src)
                add
                break

            case SynchronizedStatement:
                def add = new SynchronizedStatement(transformer.transform(src.expression), cloneStatement(src.code, transformer))
                add.setSourcePosition(src)
                add
                break

            case ThrowStatement:
                def add = new ThrowStatement(src.expression)
                add.setSourcePosition(src)
                add
                break

            case TryCatchStatement:
                def add = new TryCatchStatement(cloneStatement(src.tryStatement, transformer), cloneStatement(src.finallyStatement, transformer))
                for (c in src.catchStatements)
                    add.addCatch((CatchStatement)cloneStatement(c, transformer))
                add.setSourcePosition(src)
                add
                break

            case DoWhileStatement:
                def add = new WhileStatement(new BooleanExpression(transformer.transform(src.booleanExpression.expression)), cloneStatement(src.loopBlock, transformer))
                add.setSourcePosition(src)
                add
                break

            case WhileStatement:
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

    void processClass(ClassNode classNode, SourceUnit source, List<InnerClassNode> toAdd) {
        ClassNode inner = null
        Statement code = hasGrunitAnnotation(classNode, source)
        if(code) {
            inner = new InnerClassNode(classNode, classNode.name + "\$GrUnitTest", Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC, GROOVY_TEST_CASE)
            toAdd << inner
            processGrunitScript(source, inner, code, true, "test\$main")
        }

        for (methodNode in classNode.methods) {
            code = hasGrunitAnnotation(methodNode, source)
            if(code) {
                if (!inner) {
                    inner = new InnerClassNode(classNode, classNode.name + "\$GrUnitTest", Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC, GROOVY_TEST_CASE)
                    toAdd << inner
                }
                processGrunitScript(source, inner, code, true, "test\$" + methodNode.name)
            }
        }

        for (constructorNode in classNode.declaredConstructors) {
            code = hasGrunitAnnotation(constructorNode, source)
            if(code) {
                if (!inner) {
                    inner = new InnerClassNode(classNode, classNode.name + "\$GrUnitTest", Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC, GROOVY_TEST_CASE)
                    toAdd << inner
                }
                processGrunitScript(source, inner, code, false, "test\$constructor")
            }
        }

        for (c in classNode.innerClasses)
            processClass(c, source, toAdd)
    }

    String findName(String name, ClassNode classNode) {
        if(!classNode.getMethods(name)) {
            return name
        }
        else {
            def c = 0
            while(true) {
                c++
                def nm = name + "\$" + c
                if(!classNode.getMethods(nm))
                    return nm
            }
        }
    }
}
