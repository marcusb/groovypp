package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import static org.codehaus.groovy.ast.ClassHelper.*;
import org.codehaus.groovy.classgen.BytecodeExpression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.ClassGeneratorException;
import org.codehaus.groovy.classgen.Variable;
import org.codehaus.groovy.reflection.ReflectionCache;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.runtime.DefaultGroovyPPMethods;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.math.BigDecimal;
import java.math.BigInteger;

public abstract class BytecodeExpr extends BytecodeExpression implements Opcodes {
    public final void visit(MethodVisitor mv) {
        compile(mv);
    }

    public BytecodeExpr(ASTNode parent, ClassNode type) {
        setSourcePosition(parent);
        setType(type);
    }

    protected abstract void compile(MethodVisitor mv);


    public BytecodeExpr createIndexed(ASTNode parent, BytecodeExpr index, CompilerTransformer compiler) {
        if (getType().isArray() && TypeUtil.isAssignableFrom(int_TYPE, index.getType()))
            return new ResolvedArrayBytecodeExpr(parent, this, index, compiler);
        else {
            MethodNode getter = compiler.findMethod(getType(), "getAt", new ClassNode[]{index.getType()});

            if (getter == null) {
                compiler.addError("Can't find method 'getAt' for type: " + getType().getName(), parent);
                return null;
            }

            ClassNode ret = TypeUtil.getSubstitutedType(getter.getReturnType(), getter.getDeclaringClass(), getType());
            //MethodNode setter = compiler.findMethod(getType(), "putAt", new ClassNode[]{index.getType(), ret});

            return new ResolvedArrayLikeBytecodeExpr(parent, this, index, getter, compiler);
        }
    }


    public BytecodeExpr createPrefixOp(ASTNode exp, int type, CompilerTransformer compiler) {
        ClassNode vtype = getType();
        if (TypeUtil.isNumericalType(vtype)) {
            return new BytecodeExpr(exp, vtype) {
                protected void compile(MethodVisitor mv) {
                    BytecodeExpr.this.visit(mv);
                }
            };
        }

        if (ClassHelper.isPrimitiveType(vtype))
            vtype = TypeUtil.wrapSafely(vtype);

        String methodName = type == Types.PLUS_PLUS ? "next" : "previous";
        final MethodNode methodNode = compiler.findMethod(vtype, methodName, ClassNode.EMPTY_ARRAY);
        if (methodNode == null) {
            compiler.addError("Can't find method " + methodName + " for type " + vtype.getName(), exp);
            return null;
        }

        final BytecodeExpr nextCall = (BytecodeExpr) compiler.transform(new MethodCallExpression(
                new BytecodeExpr(exp, vtype) {
                    protected void compile(MethodVisitor mv) {
                    }
                },
                methodName,
                new ArgumentListExpression()
        ));

        return new BytecodeExpr(exp, vtype) {
            protected void compile(MethodVisitor mv) {
                BytecodeExpr.this.visit(mv);
                mv.visitInsn(DUP);
                nextCall.visit(mv);
                pop(nextCall.getType(), mv);
            }
        };
    }

    public BytecodeExpr createPostfixOp(ASTNode exp, int type, CompilerTransformer compiler) {
        ClassNode vtype = getType();
        if (TypeUtil.isNumericalType(vtype)) {
            return new BytecodeExpr(exp, vtype) {
                protected void compile(MethodVisitor mv) {
                    BytecodeExpr.this.visit(mv);
                }
            };
        }

        if (ClassHelper.isPrimitiveType(vtype))
            vtype = TypeUtil.wrapSafely(vtype);

        String methodName = type == Types.PLUS_PLUS ? "next" : "previous";
        final MethodNode methodNode = compiler.findMethod(vtype, methodName, ClassNode.EMPTY_ARRAY);
        if (methodNode == null) {
            compiler.addError("Can't find method " + methodName + " for type " + vtype.getName(), exp);
            return null;
        }

        final BytecodeExpr nextCall = (BytecodeExpr) compiler.transform(new MethodCallExpression(
                new BytecodeExpr(exp, vtype) {
                    protected void compile(MethodVisitor mv) {
                    }
                },
                methodName,
                new ArgumentListExpression()
        ));

        return new BytecodeExpr(exp, vtype) {
            protected void compile(MethodVisitor mv) {
                BytecodeExpr.this.visit(mv);
                mv.visitInsn(DUP);
                nextCall.visit(mv);
                pop(nextCall.getType(), mv);
            }
        };
    }

    /**
     * box the primitive value on the stack
     *
     * @param type
     * @param mv
     */
    public void quickBoxIfNecessary(ClassNode type, MethodVisitor mv) {
        String descr = getTypeDescription(type);
        if (type == boolean_TYPE) {
            boxBoolean(mv);
        } else if (isPrimitiveType(type) && type != VOID_TYPE) {
            ClassNode wrapper = TypeUtil.wrapSafely(type);
            String internName = getClassInternalName(wrapper);
            mv.visitTypeInsn(Opcodes.NEW, internName);
            mv.visitInsn(Opcodes.DUP);
            if (type == double_TYPE || type == long_TYPE) {
                mv.visitInsn(Opcodes.DUP2_X2);
                mv.visitInsn(Opcodes.POP2);
            } else {
                mv.visitInsn(Opcodes.DUP2_X1);
                mv.visitInsn(Opcodes.POP2);
            }
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, internName, "<init>", "(" + descr + ")V");
        }
    }

    public static void box(ClassNode type, MethodVisitor mv) {
        if (type.isPrimaryClassNode()) return;
        Class type1 = type.getTypeClass();
        if (ReflectionCache.getCachedClass(type1).isPrimitive && type1 != void.class) {
            String returnString = "(" + getTypeDescription(type) + ")" + getTypeDescription(TypeUtil.wrapSafely(type));
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, getClassInternalName(DefaultGroovyPPMethods.class.getName()), "box", returnString);
        }
    }

    /**
     * Generates the bytecode to unbox the current value on the stack
     */
    public static void unbox(Class type, MethodVisitor mv) {
        if (type.isPrimitive() && type != Void.TYPE) {
            String returnString = "(Ljava/lang/Object;)" + getTypeDescription(type);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    getClassInternalName(DefaultTypeTransformation.class.getName()),
                    type.getName() + "Unbox",
                    returnString);
        }
    }

    public static void unbox(ClassNode type, MethodVisitor mv) {
        if (type.isPrimaryClassNode()) return;
        unbox(type.getTypeClass(), mv);
    }

    public static String getClassInternalName(ClassNode t) {
        if (t.isPrimaryClassNode()) {
            return getClassInternalName(t.getName());
        }
        return getClassInternalName(t.getTypeClass());
    }

    public static String getClassInternalName(Class t) {
        return org.objectweb.asm.Type.getInternalName(t);
    }

    /**
     * @return the ASM internal name of the type
     */
    public static String getClassInternalName(String name) {
        return name.replace('.', '/');
    }

    /**
     * @return the ASM method type descriptor
     */
    public static String getMethodDescriptor(ClassNode returnType, Parameter[] parameters) {
        StringBuffer buffer = new StringBuffer("(");
        for (int i = 0; i < parameters.length; i++) {
            buffer.append(getTypeDescription(parameters[i].getType()));
        }
        buffer.append(")");
        buffer.append(getTypeDescription(returnType));
        return buffer.toString();
    }

    /**
     * @return the ASM method type descriptor
     */
    public static String getMethodDescriptor(Class returnType, Class[] paramTypes) {
        // lets avoid class loading
        StringBuffer buffer = new StringBuffer("(");
        for (int i = 0; i < paramTypes.length; i++) {
            buffer.append(getTypeDescription(paramTypes[i]));
        }
        buffer.append(")");
        buffer.append(getTypeDescription(returnType));
        return buffer.toString();
    }

    public static String getTypeDescription(Class c) {
        return org.objectweb.asm.Type.getDescriptor(c);
    }

    /**
     * array types are special:
     * eg.: String[]: classname: [Ljava.lang.String;
     * Object:   classname: java.lang.Object
     * int[] :   classname: [I
     * unlike getTypeDescription '.' is not replaced by '/'.
     * it seems that makes problems for
     * the class loading if '.' is replaced by '/'
     *
     * @return the ASM type description for class loading
     */
    public static String getClassLoadingTypeDescription(ClassNode c) {
        StringBuffer buf = new StringBuffer();
        boolean array = false;
        while (true) {
            if (c.isArray()) {
                buf.append('[');
                c = c.getComponentType();
                array = true;
            } else {
                if (isPrimitiveType(c)) {
                    buf.append(getTypeDescription(c));
                } else {
                    if (array) buf.append('L');
                    buf.append(c.getName());
                    if (array) buf.append(';');
                }
                return buf.toString();
            }
        }
    }

    /**
     * array types are special:
     * eg.: String[]: classname: [Ljava/lang/String;
     * int[]: [I
     *
     * @return the ASM type description
     */
    public static String getTypeDescription(ClassNode c) {
        return getTypeDescription(c, true);
    }

    /**
     * array types are special:
     * eg.: String[]: classname: [Ljava/lang/String;
     * int[]: [I
     *
     * @return the ASM type description
     */
    private static String getTypeDescription(ClassNode c, boolean end) {
        StringBuffer buf = new StringBuffer();
        ClassNode d = c;
        while (true) {
            if (isPrimitiveType(d)) {
                char car;
                if (d == int_TYPE) {
                    car = 'I';
                } else if (d == VOID_TYPE) {
                    car = 'V';
                } else if (d == boolean_TYPE) {
                    car = 'Z';
                } else if (d == byte_TYPE) {
                    car = 'B';
                } else if (d == char_TYPE) {
                    car = 'C';
                } else if (d == short_TYPE) {
                    car = 'S';
                } else if (d == double_TYPE) {
                    car = 'D';
                } else if (d == float_TYPE) {
                    car = 'F';
                } else /* long */ {
                    car = 'J';
                }
                buf.append(car);
                return buf.toString();
            } else if (d.isArray()) {
                buf.append('[');
                d = d.getComponentType();
            } else {
                buf.append('L');
                String name = d.getName();
                int len = name.length();
                for (int i = 0; i < len; ++i) {
                    char car = name.charAt(i);
                    buf.append(car == '.' ? '/' : car);
                }
                if (end) buf.append(';');
                return buf.toString();
            }
        }
    }

    /**
     * @return an array of ASM internal names of the type
     */
    public static String[] getClassInternalNames(ClassNode[] names) {
        int size = names.length;
        String[] answer = new String[size];
        for (int i = 0; i < size; i++) {
            answer[i] = getClassInternalName(names[i]);
        }
        return answer;
    }

    protected void pushConstant(boolean value, MethodVisitor mv) {
        if (value) {
            mv.visitInsn(Opcodes.ICONST_1);
        } else {
            mv.visitInsn(Opcodes.ICONST_0);
        }
    }

    public void pushConstant(int value, MethodVisitor mv) {
        switch (value) {
            case 0:
                mv.visitInsn(Opcodes.ICONST_0);
                break;
            case 1:
                mv.visitInsn(Opcodes.ICONST_1);
                break;
            case 2:
                mv.visitInsn(Opcodes.ICONST_2);
                break;
            case 3:
                mv.visitInsn(Opcodes.ICONST_3);
                break;
            case 4:
                mv.visitInsn(Opcodes.ICONST_4);
                break;
            case 5:
                mv.visitInsn(Opcodes.ICONST_5);
                break;
            default:
                if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                    mv.visitIntInsn(Opcodes.BIPUSH, value);
                } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                    mv.visitIntInsn(Opcodes.SIPUSH, value);
                } else {
                    mv.visitLdcInsn(Integer.valueOf(value));
                }
        }
    }

    public void doCast(Class type, MethodVisitor mv) {
        if (type != Object.class) {
            if (type.isPrimitive() && type != Void.TYPE) {
                unbox(type, mv);
            } else {
                mv.visitTypeInsn(
                        Opcodes.CHECKCAST,
                        type.isArray() ? getTypeDescription(type) : getClassInternalName(type.getName()));
            }
        }
    }

    public void doCast(ClassNode type, MethodVisitor mv) {
        if (type == OBJECT_TYPE) return;
        if (isPrimitiveType(type) && type != VOID_TYPE) {
            unbox(type, mv);
        } else {
            mv.visitTypeInsn(
                    Opcodes.CHECKCAST,
                    type.isArray() ? getTypeDescription(type) : getClassInternalName(type));
        }
    }

    public static void load(ClassNode type, int idx, MethodVisitor mv) {
        if (type == double_TYPE) {
            mv.visitVarInsn(Opcodes.DLOAD, idx);
        } else if (type == float_TYPE) {
            mv.visitVarInsn(Opcodes.FLOAD, idx);
        } else if (type == long_TYPE) {
            mv.visitVarInsn(Opcodes.LLOAD, idx);
        } else if (
                type == boolean_TYPE
                        || type == char_TYPE
                        || type == byte_TYPE
                        || type == int_TYPE
                        || type == short_TYPE) {
            mv.visitVarInsn(Opcodes.ILOAD, idx);
        } else {
            mv.visitVarInsn(Opcodes.ALOAD, idx);
        }
    }

    public static void load(Variable v, MethodVisitor mv) {
        load(v.getType(), v.getIndex(), mv);
    }

    public void store(Variable v, boolean markStart, MethodVisitor mv) {
        ClassNode type = v.getType();
        int idx = v.getIndex();

        if (type == double_TYPE) {
            mv.visitVarInsn(Opcodes.DSTORE, idx);
        } else if (type == float_TYPE) {
            mv.visitVarInsn(Opcodes.FSTORE, idx);
        } else if (type == long_TYPE) {
            mv.visitVarInsn(Opcodes.LSTORE, idx);
        } else if (
                type == boolean_TYPE
                        || type == char_TYPE
                        || type == byte_TYPE
                        || type == int_TYPE
                        || type == short_TYPE) {
            mv.visitVarInsn(Opcodes.ISTORE, idx);
        } else {
            mv.visitVarInsn(Opcodes.ASTORE, idx);
        }
    }

    public void store(Variable v, MethodVisitor mv) {
        store(v, false, mv);
    }

    /**
     * load the constant on the operand stack. primitives auto-boxed.
     */
    void loadConstant(Object value, MethodVisitor mv) {
        if (value == null) {
            mv.visitInsn(Opcodes.ACONST_NULL);
        } else if (value instanceof String) {
            mv.visitLdcInsn(value);
        } else if (value instanceof Character) {
            String className = "java/lang/Character";
            mv.visitTypeInsn(Opcodes.NEW, className);
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(value);
            String methodType = "(C)V";
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, className, "<init>", methodType);
        } else if (value instanceof Number) {
            /** todo it would be more efficient to generate class constants */
            Number n = (Number) value;
            String className = BytecodeHelper.getClassInternalName(value.getClass().getName());
            mv.visitTypeInsn(Opcodes.NEW, className);
            mv.visitInsn(Opcodes.DUP);
            String methodType;
            if (n instanceof Integer) {
                //pushConstant(n.intValue());
                mv.visitLdcInsn(n);
                methodType = "(I)V";
            } else if (n instanceof Double) {
                mv.visitLdcInsn(n);
                methodType = "(D)V";
            } else if (n instanceof Float) {
                mv.visitLdcInsn(n);
                methodType = "(F)V";
            } else if (n instanceof Long) {
                mv.visitLdcInsn(n);
                methodType = "(J)V";
            } else if (n instanceof BigDecimal) {
                mv.visitLdcInsn(n.toString());
                methodType = "(Ljava/lang/String;)V";
            } else if (n instanceof BigInteger) {
                mv.visitLdcInsn(n.toString());
                methodType = "(Ljava/lang/String;)V";
            } else if (n instanceof Short) {
                mv.visitLdcInsn(n);
                methodType = "(S)V";
            } else if (n instanceof Byte) {
                mv.visitLdcInsn(n);
                methodType = "(B)V";
            } else {
                throw new ClassGeneratorException(
                        "Cannot generate bytecode for constant: " + value
                                + " of type: " + value.getClass().getName()
                                + ".  Numeric constant type not supported.");
            }
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, className, "<init>", methodType);
        } else if (value instanceof Boolean) {
            Boolean bool = (Boolean) value;
            String text = (bool.booleanValue()) ? "TRUE" : "FALSE";
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Boolean", text, "Ljava/lang/Boolean;");
        } else if (value instanceof Class) {
            Class vc = (Class) value;
            if (vc.getName().equals("java.lang.Void")) {
                // load nothing here for void
            } else {
                throw new ClassGeneratorException(
                        "Cannot generate bytecode for constant: " + value + " of type: " + value.getClass().getName());
            }
        } else {
            throw new ClassGeneratorException(
                    "Cannot generate bytecode for constant: " + value + " of type: " + value.getClass().getName());
        }
    }


    /**
     * load the value of the variable on the operand stack. unbox it if it's a reference
     *
     * @param variable
     * @param mv
     */
    public static void loadVar(Variable variable, MethodVisitor mv) {
        int index = variable.getIndex();
        load(variable, mv);
        box(variable.getType(), mv);
    }

    public void storeVar(Variable variable, MethodVisitor mv) {
        String type = variable.getTypeName();
        int index = variable.getIndex();
        store(variable, false, mv);
    }

    public void putField(FieldNode fld, MethodVisitor mv) {
        putField(fld, getClassInternalName(fld.getOwner()), mv);
    }

    public void putField(FieldNode fld, String ownerName, MethodVisitor mv) {
        mv.visitFieldInsn(Opcodes.PUTFIELD, ownerName, fld.getName(), getTypeDescription(fld.getType()));
    }

    public void swapObjectWith(ClassNode type, MethodVisitor mv) {
        if (type == long_TYPE || type == double_TYPE) {
            mv.visitInsn(Opcodes.DUP_X2);
            mv.visitInsn(Opcodes.POP);
        } else {
            mv.visitInsn(Opcodes.SWAP);
        }
    }

    public void swapWithObject(ClassNode type, MethodVisitor mv) {
        if (type == long_TYPE || type == double_TYPE) {
            mv.visitInsn(Opcodes.DUP2_X1);
            mv.visitInsn(Opcodes.POP2);
        } else {
            mv.visitInsn(Opcodes.SWAP);
        }
    }

    public static ClassNode boxOnPrimitive(ClassNode type) {
        if (!type.isArray()) return TypeUtil.wrapSafely(type);
        return boxOnPrimitive(type.getComponentType()).makeArray();
    }

    /**
     * convert boolean to Boolean
     * @param mv
     */
    public void boxBoolean(MethodVisitor mv) {
        Label l0 = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, l0);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Boolean", "TRUE", "Ljava/lang/Boolean;");
        Label l1 = new Label();
        mv.visitJumpInsn(Opcodes.GOTO, l1);
        mv.visitLabel(l0);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Boolean", "FALSE", "Ljava/lang/Boolean;");
        mv.visitLabel(l1);
    }

    /**
     * negate a boolean on stack. true->false, false->true
     * @param mv
     */
    public void negateBoolean(MethodVisitor mv) {
        // code to negate the primitive boolean
        Label endLabel = new Label();
        Label falseLabel = new Label();
        mv.visitJumpInsn(Opcodes.IFNE, falseLabel);
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitJumpInsn(Opcodes.GOTO, endLabel);
        mv.visitLabel(falseLabel);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitLabel(endLabel);
    }

    /**
     * load a message on the stack and remove it right away. Good for put a mark in the generated bytecode for debugging purpose.
     *
     * @param msg
     * @param mv
     */
    public void mark(String msg, MethodVisitor mv) {
        mv.visitLdcInsn(msg);
        mv.visitInsn(Opcodes.POP);
    }

    /**
     * returns a name that Class.forName() can take. Notablely for arrays:
     * [I, [Ljava.lang.String; etc
     * Regular object type:  java.lang.String
     *
     * @param name
     */
    public static String formatNameForClassLoading(String name) {
        if (name.equals("int")
                || name.equals("long")
                || name.equals("short")
                || name.equals("float")
                || name.equals("double")
                || name.equals("byte")
                || name.equals("char")
                || name.equals("boolean")
                || name.equals("void")
                ) {
            return name;
        }

        if (name == null) {
            return "java.lang.Object;";
        }

        if (name.startsWith("[")) {
            return name.replace('/', '.');
        }

        if (name.startsWith("L")) {
            name = name.substring(1);
            if (name.endsWith(";")) {
                name = name.substring(0, name.length() - 1);
            }
            return name.replace('/', '.');
        }

        String prefix = "";
        if (name.endsWith("[]")) { // todo need process multi
            prefix = "[";
            name = name.substring(0, name.length() - 2);
            if (name.equals("int")) {
                return prefix + "I";
            } else if (name.equals("long")) {
                return prefix + "J";
            } else if (name.equals("short")) {
                return prefix + "S";
            } else if (name.equals("float")) {
                return prefix + "F";
            } else if (name.equals("double")) {
                return prefix + "D";
            } else if (name.equals("byte")) {
                return prefix + "B";
            } else if (name.equals("char")) {
                return prefix + "C";
            } else if (name.equals("boolean")) {
                return prefix + "Z";
            } else {
                return prefix + "L" + name.replace('/', '.') + ";";
            }
        }
        return name.replace('/', '.');

    }

    public void dup(ClassNode type, MethodVisitor mv) {
        if (type == double_TYPE || type == long_TYPE)
            mv.visitInsn(Opcodes.DUP2);
        else
            mv.visitInsn(Opcodes.DUP);
    }

    public void dup_x1(ClassNode type, MethodVisitor mv) {
        if (type == double_TYPE || type == long_TYPE)
            mv.visitInsn(Opcodes.DUP2_X1);
        else
            mv.visitInsn(Opcodes.DUP_X1);
    }

    public void dup_x2(ClassNode type, MethodVisitor mv) {
        if (type == double_TYPE || type == long_TYPE)
            mv.visitInsn(Opcodes.DUP2_X2);
        else
            mv.visitInsn(Opcodes.DUP_X2);
    }

    public void pop(ClassNode type, MethodVisitor mv) {
        if (type == double_TYPE || type == long_TYPE)
            mv.visitInsn(Opcodes.POP2);
        else
            mv.visitInsn(Opcodes.POP);
    }

    public static void doReturn(MethodVisitor mv, ClassNode returnType) {
        if (returnType == double_TYPE) {
            mv.visitInsn(Opcodes.DRETURN);
        } else if (returnType == float_TYPE) {
            mv.visitInsn(Opcodes.FRETURN);
        } else if (returnType == long_TYPE) {
            mv.visitInsn(Opcodes.LRETURN);
        } else if (
                returnType == boolean_TYPE
                        || returnType == char_TYPE
                        || returnType == byte_TYPE
                        || returnType == int_TYPE
                        || returnType == short_TYPE) {
            //byte,short,boolean,int are all IRETURN
            mv.visitInsn(Opcodes.IRETURN);
        } else if (returnType == VOID_TYPE) {
            mv.visitInsn(Opcodes.RETURN);
        } else {
            mv.visitInsn(Opcodes.ARETURN);
        }

    }

    public void doReturn(ClassNode returnType, MethodVisitor mv) {
        doReturn(mv, returnType);
    }

    private static boolean hasGenerics(Parameter[] param) {
        if (param.length == 0) return false;
        for (int i = 0; i < param.length; i++) {
            ClassNode type = param[i].getType();
            if (type.getGenericsTypes() != null) return true;
        }
        return false;
    }

    public static String getGenericsMethodSignature(MethodNode node) {
        GenericsType[] generics = node.getGenericsTypes();
        Parameter[] param = node.getParameters();
        ClassNode returnType = node.getReturnType();

        if (generics == null && !hasGenerics(param) && returnType.getGenericsTypes() == null) return null;

        StringBuffer ret = new StringBuffer(100);
        getGenericsTypeSpec(ret, generics);

        GenericsType[] paramTypes = new GenericsType[param.length];
        for (int i = 0; i < param.length; i++) {
            ClassNode pType = param[i].getType();
            if (pType.getGenericsTypes() == null || !pType.isGenericsPlaceHolder()) {
                paramTypes[i] = new GenericsType(pType);
            } else {
                paramTypes[i] = pType.getGenericsTypes()[0];
            }
        }
        addSubTypes(ret, paramTypes, "(", ")");
        if (returnType.isGenericsPlaceHolder()) {
            addSubTypes(ret, returnType.getGenericsTypes(), "", "");
        } else {
            writeGenericsBounds(ret, new GenericsType(returnType), false);
        }
        return ret.toString();
    }

    private static boolean usesGenericsInClassSignature(ClassNode node) {
        if (!node.isUsingGenerics()) return false;
        if (node.getGenericsTypes() != null) return true;
        ClassNode sclass = node.getUnresolvedSuperClass(false);
        if (sclass.isUsingGenerics()) return true;
        ClassNode[] interfaces = node.getInterfaces();
        if (interfaces != null) {
            for (int i = 0; i < interfaces.length; i++) {
                if (interfaces[i].isUsingGenerics()) return true;
            }
        }

        return false;
    }

    public static String getGenericsSignature(ClassNode node) {
        if (!usesGenericsInClassSignature(node)) return null;
        GenericsType[] genericsTypes = node.getGenericsTypes();
        StringBuffer ret = new StringBuffer(100);
        getGenericsTypeSpec(ret, genericsTypes);
        GenericsType extendsPart = new GenericsType(node.getUnresolvedSuperClass(false));
        writeGenericsBounds(ret, extendsPart, true);
        ClassNode[] interfaces = node.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            GenericsType interfacePart = new GenericsType(interfaces[i]);
            writeGenericsBounds(ret, interfacePart, false);
        }
        return ret.toString();
    }

    private static void getGenericsTypeSpec(StringBuffer ret, GenericsType[] genericsTypes) {
        if (genericsTypes == null) return;
        ret.append('<');
        for (int i = 0; i < genericsTypes.length; i++) {
            String name = genericsTypes[i].getName();
            ret.append(name);
            ret.append(':');
            writeGenericsBounds(ret, genericsTypes[i], true);
        }
        ret.append('>');
    }

    public static String getGenericsBounds(ClassNode type) {
        GenericsType[] genericsTypes = type.getGenericsTypes();
        if (genericsTypes == null) return null;
        StringBuffer ret = new StringBuffer(100);
        if (type.isGenericsPlaceHolder()) {
            addSubTypes(ret, type.getGenericsTypes(), "", "");
        } else {
            GenericsType gt = new GenericsType(type);
            writeGenericsBounds(ret, gt, false);
        }

        return ret.toString();
    }

    private static void writeGenericsBoundType(StringBuffer ret, ClassNode printType, boolean writeInterfaceMarker) {
        if (writeInterfaceMarker && printType.isInterface()) ret.append(":");
        ret.append(getTypeDescription(printType, false));
        addSubTypes(ret, printType.getGenericsTypes(), "<", ">");
        if (!isPrimitiveType(printType)) ret.append(";");
    }

    private static void writeGenericsBounds(StringBuffer ret, GenericsType type, boolean writeInterfaceMarker) {
        if (type.getUpperBounds() != null) {
            ClassNode[] bounds = type.getUpperBounds();
            for (int i = 0; i < bounds.length; i++) {
                writeGenericsBoundType(ret, bounds[i], writeInterfaceMarker);
            }
        } else if (type.getLowerBound() != null) {
            writeGenericsBoundType(ret, type.getLowerBound(), writeInterfaceMarker);
        } else {
            writeGenericsBoundType(ret, type.getType(), writeInterfaceMarker);
        }
    }

    private static void addSubTypes(StringBuffer ret, GenericsType[] types, String start, String end) {
        if (types == null) return;
        ret.append(start);
        for (int i = 0; i < types.length; i++) {
            String name = types[i].getName();
            if (types[i].isPlaceholder()) {
                ret.append('T');
                ret.append(name);
                ret.append(';');
            } else if (types[i].isWildcard()) {
                if (types[i].getUpperBounds() != null) {
                    ret.append('+');
                    writeGenericsBounds(ret, types[i], false);
                } else if (types[i].getLowerBound() != null) {
                    ret.append('-');
                    writeGenericsBounds(ret, types[i], false);
                } else {
                    ret.append('*');
                }
            } else {
                writeGenericsBounds(ret, types[i], false);
            }
        }
        ret.append(end);
    }

    public static boolean isIntegralType(ClassNode expr) {
        return expr.equals(Integer_TYPE) || expr.equals(Byte_TYPE) || expr.equals(Short_TYPE);
    }

    public static void cast(ClassNode expr, ClassNode type, MethodVisitor mv) {
        if (isPrimitiveType(expr) || isPrimitiveType(type)) {
            throw new RuntimeException("Can't convert " + expr.getName() + " to " + type.getName());
        }

        expr = expr.redirect();
        type = type.redirect();

        if (TypeUtil.isDirectlyAssignableFrom(type, expr)) {
            return;
        }

        if (expr.isArray()) {
            castArray (expr, type, mv);
        } else if (isIntegralType(expr)) {
            castIntegral(expr, type, mv);
        } else if (expr == Character_TYPE) {
            unbox(getUnwrapper(expr), mv);
            box (int_TYPE, mv);
            castIntegral(Integer_TYPE, type, mv);
        } else if (expr == Boolean_TYPE) {
            unbox(getUnwrapper(expr), mv);
            box (int_TYPE, mv);
            castIntegral(Integer_TYPE, type, mv);
        } else if (expr == Long_TYPE) {
            castLong(expr, type, mv);
        } else if (expr == Double_TYPE) {
            castDouble(expr, type, mv);
        } else if (expr == Float_TYPE) {
            castFloat(expr, type, mv);
        } else if (expr == BigDecimal_TYPE) {
            castBigDecimal(expr, type, mv);
        } else if (expr == BigInteger_TYPE) {
            castBigInteger(expr, type, mv);
        } else if (expr == STRING_TYPE) {
            castString(expr, type, mv);
        } else if (expr.equals(TypeUtil.Number_TYPE)) {
            castNumber(expr, type, mv);
        } else if (expr.implementsInterface(TypeUtil.COLLECTION_TYPE)) {
            castCollection(expr, type, mv);
        } else {
            if (TypeUtil.isNumericalType(type) && !type.equals(TypeUtil.Number_TYPE)) {
                Label nullLabel = new Label(), doneLabel = new Label();
                mv.visitInsn(DUP);
                mv.visitJumpInsn(IFNULL, nullLabel);
                unbox(getUnwrapper(type), mv);
                box(getUnwrapper(type), mv);
                mv.visitJumpInsn(GOTO, doneLabel);
                mv.visitLabel(nullLabel);
                checkCast(type, mv);
                mv.visitLabel(doneLabel);
            } else {
                if (expr != TypeUtil.NULL_TYPE) {
                    if (type.equals(STRING_TYPE)) {
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
                    } else {
                        BytecodeExpr.checkCast(type, mv);
                    }
                }
            }
        }
    }

    private static void castArray(ClassNode expr, ClassNode type, MethodVisitor mv) {
        mv.visitLdcInsn(BytecodeHelper.getClassLoadingTypeDescription(type));
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
        mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/ScriptBytecodeAdapter", "asType", "(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;");
        BytecodeExpr.checkCast(type, mv);
    }

    private static void castNumber(ClassNode expr, ClassNode type, MethodVisitor mv) {
        if (type == Integer_TYPE) {
            unbox(int_TYPE, mv);
            box(int_TYPE, mv);
        } else if (type == Boolean_TYPE) {
            unbox(int_TYPE, mv);
            box(boolean_TYPE, mv);
        } else if (type == Byte_TYPE) {
            unbox(int_TYPE, mv);
            box(byte_TYPE, mv);
        } else if (type == Short_TYPE) {
            unbox(int_TYPE, mv);
            box(short_TYPE, mv);
        } else if (type == Character_TYPE) {
            unbox(int_TYPE, mv);
            box(char_TYPE, mv);
        } else if (type == Long_TYPE) {
            unbox(long_TYPE, mv);
            box(long_TYPE, mv);
        } else if (type == Float_TYPE) {
            unbox(float_TYPE, mv);
            box(float_TYPE, mv);
        } else if (type == Double_TYPE) {
            unbox(double_TYPE, mv);
            box(double_TYPE, mv);
        } else if (type == BigDecimal_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigDecimal");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigDecimal", "<init>", "(Ljava/lang/String;)V");
        } else if (type == BigInteger_TYPE) {
            if (expr.equals(Character_TYPE)) {
                mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C");
                box(int_TYPE, mv);
            }
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigInteger");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigInteger", "<init>", "(Ljava/lang/String;)V");
        } else {
            BytecodeExpr.checkCast(type, mv);
        }
    }

    private static void castCollection(ClassNode expr, ClassNode type, MethodVisitor mv) {
        if (type.isArray()) {
            if (!ClassHelper.isPrimitiveType(type.getComponentType())) {
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "size", "()I");
                mv.visitTypeInsn(ANEWARRAY, BytecodeHelper.getClassInternalName(type.getComponentType()));
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;");
                BytecodeExpr.checkCast(type, mv);
                return;
            } else {
                mv.visitLdcInsn(BytecodeHelper.getClassLoadingTypeDescription(type));
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/typehandling/DefaultTypeTransformation", "asArray", "(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;");
                BytecodeExpr.checkCast(type, mv);
                return;
            }
        }

        throw new IllegalStateException("Impossible cast");
    }

    public static void checkCast(ClassNode type, MethodVisitor mv) {
        mv.visitTypeInsn(CHECKCAST, type.isArray() ? BytecodeHelper.getTypeDescription(type): BytecodeHelper.getClassInternalName(type));
    }

    private static void castString(ClassNode expr, ClassNode type, MethodVisitor mv) {
        if (TypeUtil.isNumericalType(type)) {
            mv.visitInsn(ICONST_0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C");
            box(int_TYPE, mv);
            castIntegral(ClassHelper.Integer_TYPE, type, mv);
        } else if (type == Character_TYPE) {
            mv.visitInsn(ICONST_0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C");
            box(char_TYPE, mv);
        } else if (type == char_TYPE) {
            mv.visitInsn(ICONST_0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C");
        } else if (type == Boolean_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/DefaultGroovyMethods", "asBoolean", "(Ljava/lang/CharSequence;)Z");
            box(boolean_TYPE, mv);
        } else if (type == boolean_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/DefaultGroovyMethods", "asBoolean", "(Ljava/lang/CharSequence;)Z");
        } else
            throw new IllegalStateException("Impossible cast");
    }

    private static void castIntegral(ClassNode expr, ClassNode type, MethodVisitor mv) {
        if (type == Integer_TYPE) {
            unbox(getUnwrapper(expr), mv);
            box(int_TYPE, mv);
        } else if (type == Boolean_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IAND);
            box(boolean_TYPE, mv);
        } else if (type == Byte_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(I2B);
            box(byte_TYPE, mv);
        } else if (type == Short_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(I2S);
            box(short_TYPE, mv);
        } else if (type == Character_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(I2C);
            box(char_TYPE, mv);
        } else if (type == Long_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(I2L);
            box(long_TYPE, mv);
        } else if (type == Float_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(I2F);
            box(float_TYPE, mv);
        } else if (type == Double_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(I2D);
            box(double_TYPE, mv);
        } else if (type == BigDecimal_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigDecimal");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigDecimal", "<init>", "(Ljava/lang/String;)V");
        } else if (type == BigInteger_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigInteger");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigInteger", "<init>", "(Ljava/lang/String;)V");
        } else {
            BytecodeExpr.checkCast(type, mv);
        }
    }

    private static void castLong(ClassNode expr, ClassNode type, MethodVisitor mv) {
        if (type == Integer_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(L2I);
            box(int_TYPE, mv);
        } else if (type == Boolean_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(L2I);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IAND);
            box(boolean_TYPE, mv);
        } else if (type == Byte_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(L2I);
            mv.visitInsn(I2B);
            box(byte_TYPE, mv);
        } else if (type == Short_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(L2I);
            mv.visitInsn(I2S);
            box(short_TYPE, mv);
        } else if (type == Character_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(L2I);
            mv.visitInsn(I2C);
            box(char_TYPE, mv);
        } else if (type == Long_TYPE) {
        } else if (type == Float_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(L2F);
            box(float_TYPE, mv);
        } else if (type == Double_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(L2D);
            box(double_TYPE, mv);
        } else if (type == BigDecimal_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigDecimal");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigDecimal", "<init>", "(Ljava/lang/String;)V");
        } else if (type == BigInteger_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigInteger");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigInteger", "<init>", "(Ljava/lang/String;)V");
        } else {
            BytecodeExpr.checkCast(type, mv);
        }
    }

    private static void castDouble(ClassNode expr, ClassNode type, MethodVisitor mv) {
        if (type == Integer_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(D2I);
            box(int_TYPE, mv);
        } else if (type == Boolean_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(D2I);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IAND);
            box(boolean_TYPE, mv);
        } else if (type == Byte_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(D2I);
            mv.visitInsn(I2B);
            box(byte_TYPE, mv);
        } else if (type == Short_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(D2I);
            mv.visitInsn(I2S);
            box(short_TYPE, mv);
        } else if (type == Character_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(D2I);
            mv.visitInsn(I2C);
            box(char_TYPE, mv);
        } else if (type == Long_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(D2L);
            box(long_TYPE, mv);
        } else if (type == Float_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(D2F);
            box(float_TYPE, mv);
        } else if (type == Double_TYPE) {
        } else if (type == BigDecimal_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigDecimal");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigDecimal", "<init>", "(Ljava/lang/String;)V");
        } else if (type == BigInteger_TYPE) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J");
            box(long_TYPE, mv);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigInteger");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigInteger", "<init>", "(Ljava/lang/String;)V");
        } else {
            BytecodeExpr.checkCast(type, mv);
        }
    }

    private static void castFloat(ClassNode expr, ClassNode type, MethodVisitor mv) {
        if (type == Integer_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(F2I);
            box(int_TYPE, mv);
        } else if (type == Boolean_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(F2I);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IAND);
            box(boolean_TYPE, mv);
        } else if (type == Byte_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(F2I);
            mv.visitInsn(I2B);
            box(byte_TYPE, mv);
        } else if (type == Short_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(F2I);
            mv.visitInsn(I2S);
            box(short_TYPE, mv);
        } else if (type == Character_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(F2I);
            mv.visitInsn(I2C);
            box(char_TYPE, mv);
        } else if (type == Long_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(F2L);
            box(long_TYPE, mv);
        } else if (type == Float_TYPE) {
        } else if (type == Double_TYPE) {
            unbox(getUnwrapper(expr), mv);
            mv.visitInsn(F2D);
            box(double_TYPE, mv);
        } else if (type == BigDecimal_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigDecimal");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigDecimal", "<init>", "(Ljava/lang/String;)V");
        } else if (type == BigInteger_TYPE) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J");
            box(long_TYPE, mv);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigInteger");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigInteger", "<init>", "(Ljava/lang/String;)V");
        } else {
            BytecodeExpr.checkCast(type, mv);
        }
    }

    private static void castBigDecimal(ClassNode expr, ClassNode type, MethodVisitor mv) {
        if (type == Integer_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            box(int_TYPE, mv);
        } else if (type == Boolean_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IAND);
            box(boolean_TYPE, mv);
        } else if (type == Byte_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            mv.visitInsn(I2B);
            box(byte_TYPE, mv);
        } else if (type == Short_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            mv.visitInsn(I2S);
            box(short_TYPE, mv);
        } else if (type == Character_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            mv.visitInsn(I2C);
            box(char_TYPE, mv);
        } else if (type == Long_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J");
            box(long_TYPE, mv);
        } else if (type == Float_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F");
            box(float_TYPE, mv);
        } else if (type == Double_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D");
            box(double_TYPE, mv);
        } else if (type == BigDecimal_TYPE) {
        } else if (type == BigInteger_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/math/BigDecimal", "toBigInteger", "()Ljava/math/BigInteger;");
        } else {
            BytecodeExpr.checkCast(type, mv);
        }
    }

    private static void castBigInteger(ClassNode expr, ClassNode type, MethodVisitor mv) {
        if (type == Integer_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            box(int_TYPE, mv);
        } else if (type == Boolean_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IAND);
            box(boolean_TYPE, mv);
        } else if (type == Byte_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            mv.visitInsn(I2B);
            box(byte_TYPE, mv);
        } else if (type == Short_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            mv.visitInsn(I2S);
            box(short_TYPE, mv);
        } else if (type == Character_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            mv.visitInsn(I2C);
            box(char_TYPE, mv);
        } else if (type == Long_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J");
            box(long_TYPE, mv);
        } else if (type == Float_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F");
            box(float_TYPE, mv);
        } else if (type == Double_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D");
            box(double_TYPE, mv);
        } else if (type == BigDecimal_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigDecimal");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigDecimal", "<init>", "(Ljava/lang/String;)V");
        } else if (type == BigInteger_TYPE) {
        } else {
            BytecodeExpr.checkCast(type, mv);
        }
    }

    protected void incOrDecPrimitive(ClassNode primType, final int op, MethodVisitor mv) {
        boolean add = op == Types.PLUS_PLUS;
        if (primType == BigDecimal_TYPE || primType == BigInteger_TYPE) {
            if (add)
                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/DefaultGroovyMethods", "next", "(Ljava/lang/Number;)Ljava/lang/Number;");
            else
                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/DefaultGroovyMethods", "previous", "(Ljava/lang/Number;)Ljava/lang/Number;");
            BytecodeExpr.checkCast(primType, mv);
        } else if (primType == double_TYPE) {
            mv.visitInsn(DCONST_1);
            mv.visitInsn(add ? DADD : DSUB);
        } else if (primType == long_TYPE) {
            mv.visitInsn(LCONST_1);
            mv.visitInsn(add ? LADD : LSUB);
        } else if (primType == float_TYPE) {
            mv.visitInsn(FCONST_1);
            mv.visitInsn(add ? FADD : FSUB);
        } else {
            mv.visitInsn(ICONST_1);
            mv.visitInsn(add ? IADD : ISUB);
        }
    }

    protected void toInt(ClassNode type, MethodVisitor mv) {
        if (isPrimitiveType(type)) {
            if (type == double_TYPE) {
                mv.visitInsn(D2I);
            } else if (type == long_TYPE) {
                mv.visitInsn(L2I);
            } else if (type == float_TYPE) {
                mv.visitInsn(F2I);
            }
        } else {
            unbox(int_TYPE, mv);
        }
    }

    protected void loadArray(ClassNode type, MethodVisitor mv) {
        if (type == byte_TYPE) {
            mv.visitInsn(BALOAD);
        } else if (type == char_TYPE) {
            mv.visitInsn(CALOAD);
        } else if (type == short_TYPE) {
            mv.visitInsn(SALOAD);
        } else if (type == int_TYPE) {
            mv.visitInsn(IALOAD);
        } else if (type == long_TYPE) {
            mv.visitInsn(LALOAD);
        } else if (type == float_TYPE) {
            mv.visitInsn(FALOAD);
        } else if (type == double_TYPE) {
            mv.visitInsn(DALOAD);
        } else {
            mv.visitInsn(AALOAD);
        }
    }

    protected void storeArray(ClassNode type, MethodVisitor mv) {
        if (type == byte_TYPE) {
            mv.visitInsn(BASTORE);
        } else if (type == char_TYPE) {
            mv.visitInsn(CASTORE);
        } else if (type == short_TYPE) {
            mv.visitInsn(SASTORE);
        } else if (type == int_TYPE) {
            mv.visitInsn(IASTORE);
        } else if (type == long_TYPE) {
            mv.visitInsn(LASTORE);
        } else if (type == float_TYPE) {
            mv.visitInsn(FASTORE);
        } else if (type == double_TYPE) {
            mv.visitInsn(DASTORE);
        } else {
            mv.visitInsn(AASTORE);
        }
    }

    public boolean isThis() {
        return false;
    }
}
