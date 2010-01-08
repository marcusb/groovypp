package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.objectweb.asm.Opcodes;


/**
 * Handles generation of code for the @Trait annotation
 *
 * @author 2008-2009 Copyright (C) MBTE Sweden AB. All Rights Reserved.
 */
@GroovyASTTransformation(phase = CompilePhase.INSTRUCTION_SELECTION)
public class VerificationASTTransform implements ASTTransformation, Opcodes {
    ModuleNode module;

    public void visit(ASTNode[] nodes, final SourceUnit source) {
        module = (ModuleNode) nodes[0];

        for (ClassNode classNode : module.getClasses()) {
            try {
                new OpenVerifier().visitClass(classNode);
            }
            catch (MultipleCompilationErrorsException err) {
                throw err;
            }
            catch (Throwable t) {
                int line = classNode.getLineNumber();
                int col = classNode.getColumnNumber();
                source.getErrorCollector().addError(new SyntaxErrorMessage(new SyntaxException(t.getMessage() + '\n', line, col), source), true);
            }
        }
    }

}