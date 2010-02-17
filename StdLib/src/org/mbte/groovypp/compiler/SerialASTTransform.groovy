package org.mbte.groovypp.compiler

import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.ast.*
import static org.codehaus.groovy.ast.ClassHelper.make
import org.objectweb.asm.Opcodes

import groovy.util.concurrent.CallLater
import groovy.util.concurrent.BindLater
import java.util.concurrent.Executor
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
import org.codehaus.groovy.syntax.SyntaxException
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.PropertyExpression

@Typed
@GroovyASTTransformation (phase = CompilePhase.CANONICALIZATION)
class SerialASTTransform implements ASTTransformation, Opcodes {
    static final ClassNode EXTERNALIZABLE = make(Externalizable)
    static final ClassNode OBJECT_INPUT = make(ObjectInput)
    static final ClassNode OBJECT_OUTPUT = make(ObjectOutput)

    static final Parameter [] readExternalParams = [[OBJECT_INPUT, "__input__"]]
    static final Parameter [] writeExternalParams = [[OBJECT_OUTPUT, "__output__"]]

    void visit(ASTNode[] nodes, SourceUnit source) {
        ModuleNode module = nodes[0]
        for (ClassNode classNode: module.classes) {

            if (!classNode.implementsInterface(EXTERNALIZABLE))
                continue;

            new OpenVerifier().visitClass(classNode);

            def hasDefConstructor = false
            for(constructor in classNode.declaredConstructors) {
                if (!constructor.parameters.length && constructor.public) {
                    hasDefConstructor = true
                    break
                }
            }

            if (!hasDefConstructor) {
                source.addError(new SyntaxException("Class implementing java.io.Externalizable must have public no-arg constructor", classNode.lineNumber, classNode.columnNumber))
            }

            def readMethod = classNode.getDeclaredMethod("readExternal", readExternalParams)
            def writeMethod = classNode.getDeclaredMethod("writeExternal", writeExternalParams)

            if (!readMethod && !writeMethod) {
                addReadWriteExternal(classNode)
            }
        }
    }

    private def addReadWriteExternal(ClassNode classNode) {
        def readCode = new BlockStatement()
        if (classNode.superClass != ClassHelper.OBJECT_TYPE)
            readCode.addStatement(new ExpressionStatement(new MethodCallExpression(VariableExpression.SUPER_EXPRESSION, "readExternal", new ArgumentListExpression(new VariableExpression("__input__")))))

        def writeCode = new BlockStatement()
        if (classNode.superClass != ClassHelper.OBJECT_TYPE)
            writeCode.addStatement(new ExpressionStatement(new MethodCallExpression(VariableExpression.SUPER_EXPRESSION, "writeExternal", new ArgumentListExpression(new VariableExpression("__output__")))))

        for (f in classNode.fields) {
            if (f.static || (f.modifiers & ACC_TRANSIENT) != 0)
                continue

            def tname = f.type.name
            if (ClassHelper.isPrimitiveType(f.type))
                tname = tname[0].toUpperCase() + tname.substring(1)
            else {
                if (f.type == ClassHelper.STRING_TYPE)
                    tname = "UTF"
                else
                    tname = "Object"
            }

            // this.prop = __input__.readXXX()
            readCode.addStatement(new ExpressionStatement(
                    new BinaryExpression(
                            new PropertyExpression(VariableExpression.THIS_EXPRESSION, f.name),
                            Token.newSymbol(Types.ASSIGN, -1, -1),
                            new MethodCallExpression(
                                    new VariableExpression(readExternalParams[0]),
                                    "read" + tname,
                                    new ArgumentListExpression()
                            )
                    )
            ))
            // __output__.writeXXX(this.prop)
            writeCode.addStatement(new ExpressionStatement(
                    new MethodCallExpression(
                            new VariableExpression(writeExternalParams[0]),
                            "write" + tname,
                            new ArgumentListExpression(
                                    new PropertyExpression(VariableExpression.THIS_EXPRESSION, f.name),
                            )
                    )
            ))
        }

        def readMethod = classNode.addMethod("readExternal", ACC_PUBLIC, ClassHelper.VOID_TYPE, readExternalParams, ClassNode.EMPTY_ARRAY, readCode)
        readMethod.addAnnotation(new AnnotationNode(TypeUtil.TYPED))

        def writeMethod = classNode.addMethod("writeExternal", ACC_PUBLIC, ClassHelper.VOID_TYPE, writeExternalParams, ClassNode.EMPTY_ARRAY, writeCode)
        writeMethod.addAnnotation(new AnnotationNode(TypeUtil.TYPED))
    }
}
