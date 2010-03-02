package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.LinkedList;

public class VolatileFieldUpdaterTransform {
    public static void addUpdaterForVolatileFields(ClassNode classNode) {
        List<FieldNode> toAdd = null;
        for (FieldNode fieldNode : classNode.getFields()) {
            if((fieldNode.getModifiers() & Opcodes.ACC_VOLATILE) != 0) {
                final String fieldName = fieldNode.getName() + "$updater";
                if(classNode.getDeclaredField(fieldName) != null)
                    continue;
                if (fieldNode.getType().equals(ClassHelper.int_TYPE)) {
                    ClassNode type = TypeUtil.withGenericTypes(TypeUtil.ATOMIC_INTEGER_FIELD_UPDATER, classNode);
                    FieldNode newField = new FieldNode(fieldName, Opcodes.ACC_PUBLIC| Opcodes.ACC_STATIC| Opcodes.ACC_FINAL, type, classNode, null);
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
                    FieldNode newField = new FieldNode(fieldName, Opcodes.ACC_PUBLIC| Opcodes.ACC_STATIC| Opcodes.ACC_FINAL, type, classNode, null);
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
                    FieldNode newField = new FieldNode(fieldName, Opcodes.ACC_PUBLIC| Opcodes.ACC_STATIC| Opcodes.ACC_FINAL, type, classNode, null);
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
}
