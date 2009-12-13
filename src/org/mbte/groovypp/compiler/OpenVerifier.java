package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.Verifier;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.reflection.CachedField;
import org.codehaus.groovy.reflection.ReflectionCache;
import org.objectweb.asm.Opcodes;
import org.mbte.groovypp.compiler.transformers.MethodCallExpressionTransformer;

import java.util.List;
import java.util.LinkedList;

class OpenVerifier extends Verifier {
    static CachedField classNodeField = null;

    static {
        for (CachedField f : ReflectionCache.getCachedClass(Verifier.class).getFields()) {
            if (f.getName().equals("classNode")) {
                classNodeField = f;
                break;
            }
        }
    }

    @Override
    public void visitClass(ClassNode classNode) {
        addUpdaterForVolatileFields(classNode);

        super.visitClass(classNode);

        for (FieldNode fieldNode : classNode.getFields()) {
            fieldNode.setInitialValueExpression(null);
        }
    }

    private void addUpdaterForVolatileFields(ClassNode classNode) {
        List<FieldNode> toAdd = null;
        for (FieldNode fieldNode : classNode.getFields()) {
            if((fieldNode.getModifiers() & Opcodes.ACC_VOLATILE) != 0) {
                if (fieldNode.getType().equals(ClassHelper.int_TYPE)) {
                    ClassNode type = TypeUtil.withGenericTypes(TypeUtil.ATOMIC_INTEGER_FIELD_UPDATER, classNode);
                    FieldNode newField = new FieldNode(fieldNode.getName() + "$updater", ACC_PUBLIC|ACC_STATIC|ACC_FINAL, type, classNode, null);
                    newField.setInitialValueExpression(
                            new StaticMethodCallExpression(
                                    TypeUtil.ATOMIC_INTEGER_FIELD_UPDATER,
                                    "newUpdater",
                                    new ArgumentListExpression(
                                            new ClassExpression(classNode),
                                            new ConstantExpression(fieldNode.getName())
                                    )
                            )
                    );
                    if (toAdd == null)
                        toAdd = new LinkedList<FieldNode>();
                    toAdd.add(newField);
                    continue;
                }

                if (fieldNode.getType().equals(ClassHelper.long_TYPE)) {
                    ClassNode type = TypeUtil.withGenericTypes(TypeUtil.ATOMIC_LONG_FIELD_UPDATER, classNode);
                    FieldNode newField = new FieldNode(fieldNode.getName() + "$updater", ACC_PUBLIC|ACC_STATIC|ACC_FINAL, type, classNode, null);
                    newField.setInitialValueExpression(
                            new StaticMethodCallExpression(
                                    TypeUtil.ATOMIC_LONG_FIELD_UPDATER,
                                    "newUpdater",
                                    new ArgumentListExpression(
                                            new ClassExpression(classNode),
                                            new ConstantExpression(fieldNode.getName())
                                    )
                            )
                    );
                    if (toAdd == null)
                        toAdd = new LinkedList<FieldNode> ();
                    toAdd.add(newField);
                    continue;
                }

                if (!ClassHelper.isPrimitiveType(fieldNode.getType())) {
                    ClassNode type = TypeUtil.withGenericTypes(TypeUtil.ATOMIC_REFERENCE_FIELD_UPDATER, classNode,  fieldNode.getType());
                    FieldNode newField = new FieldNode(fieldNode.getName() + "$updater", ACC_PUBLIC|ACC_STATIC|ACC_FINAL, type, classNode, null);
                    newField.setInitialValueExpression(
                            new StaticMethodCallExpression(
                                    TypeUtil.ATOMIC_REFERENCE_FIELD_UPDATER,
                                    "newUpdater",
                                    new ArgumentListExpression(
                                            new ClassExpression(classNode),
                                            new ClassExpression(fieldNode.getType().redirect()),
                                            new ConstantExpression(fieldNode.getName())
                                    )
                            )
                    );
                    if (toAdd == null)
                        toAdd = new LinkedList<FieldNode> ();
                    toAdd.add(newField);
                }
            }
        }

        if (toAdd != null)
            for (FieldNode fieldNode : toAdd) {
                classNode.addField(fieldNode);
            }
    }

    public void addPropertyMethods(PropertyNode node) {
        classNodeField.setProperty(this, node.getDeclaringClass());
        visitProperty(node);
    }

    public void visitMethod(MethodNode node) {
    }

    protected void addInitialization(ClassNode node, ConstructorNode constructorNode) {
        if (constructorNode.getCode() instanceof BytecodeSequence)
            return;

        super.addInitialization(node, constructorNode);
    }
}
