package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.classgen.BytecodeExpression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.ClassGeneratorException;
import org.codehaus.groovy.classgen.Variable;
import org.codehaus.groovy.reflection.ReflectionCache;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.math.BigDecimal;
import java.math.BigInteger;

public abstract class BytecodeExpr extends BytecodeExpression implements Opcodes{
    protected MethodVisitor mv;

    public final void visit(MethodVisitor mv) {
        this.mv = mv;
        compile ();
    }

    public BytecodeExpr (ASTNode parent, ClassNode type) {
        setSourcePosition(parent);
        setType(type);
    }

    protected abstract void compile();


    public BytecodeExpr createIndexed(ASTNode parent, BytecodeExpr index, CompilerTransformer compiler) {
        if (getType().isArray() && TypeUtil.isAssignableFrom(ClassHelper.int_TYPE, index.getType()))
           return new ResolvedArrayBytecodeExpr(parent, this, index);
        else {
            MethodNode getter = compiler.findMethod(getType(), "getAt", new ClassNode[]{index.getType()});
            MethodNode setter = compiler.findMethod(getType(), "putAt", new ClassNode[]{index.getType()});

            if (getter == null) {
                compiler.addError("Can't find method 'getAt' for type: " + getType().getName(), parent);
                return null;
            }

            return new ResolvedArrayLikeBytecodeExpr(parent, this, index, getter, setter);
        }
    }


    public BytecodeExpr createPrefixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }

    public BytecodeExpr createPostfixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }

    /**
     * box the primitive value on the stack
     *
     * @param type
     */
    public void quickBoxIfNecessary(ClassNode type) {
        String descr = getTypeDescription(type);
        if (type == ClassHelper.boolean_TYPE) {
            boxBoolean();
        } else if (ClassHelper.isPrimitiveType(type) && type != ClassHelper.VOID_TYPE) {
            ClassNode wrapper = ClassHelper.getWrapper(type);
            String internName = getClassInternalName(wrapper);
            mv.visitTypeInsn(Opcodes.NEW, internName);
            mv.visitInsn(Opcodes.DUP);
            if (type == ClassHelper.double_TYPE || type == ClassHelper.long_TYPE) {
                mv.visitInsn(Opcodes.DUP2_X2);
                mv.visitInsn(Opcodes.POP2);
            } else {
                mv.visitInsn(Opcodes.DUP2_X1);
                mv.visitInsn(Opcodes.POP2);
            }
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, internName, "<init>", "(" + descr + ")V");
        }
    }

    public void quickUnboxIfNecessary(ClassNode type) {
        if (ClassHelper.isPrimitiveType(type) && type != ClassHelper.VOID_TYPE) { // todo care when BigDecimal or BigIneteger on stack
            ClassNode wrapper = ClassHelper.getWrapper(type);
            String internName = getClassInternalName(wrapper);
            if (type == ClassHelper.boolean_TYPE) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, internName);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internName, type.getName() + "Value", "()" + getTypeDescription(type));
            } else { // numbers
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Number");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, /*internName*/"java/lang/Number", type.getName() + "Value", "()" + getTypeDescription(type));
            }
        }
    }

    /**
     * Generates the bytecode to autobox the current value on the stack
     */
    public void box(Class type) {
        if (ReflectionCache.getCachedClass(type).isPrimitive && type != void.class) {
            String returnString = "(" + getTypeDescription(type) + ")Ljava/lang/Object;";
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, getClassInternalName(DefaultTypeTransformation.class.getName()), "box", returnString);
        }
    }

    public void box(ClassNode type) {
        if (type.isPrimaryClassNode()) return;
        box(type.getTypeClass());
    }

    /**
     * Generates the bytecode to unbox the current value on the stack
     */
    public void unbox(Class type) {
        if (type.isPrimitive() && type != Void.TYPE) {
            String returnString = "(Ljava/lang/Object;)" + getTypeDescription(type);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    getClassInternalName(DefaultTypeTransformation.class.getName()),
                    type.getName() + "Unbox",
                    returnString);
        }
    }

    public void unbox(ClassNode type) {
        if (type.isPrimaryClassNode()) return;
        unbox(type.getTypeClass());
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
                if (ClassHelper.isPrimitiveType(c)) {
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
            if (ClassHelper.isPrimitiveType(d)) {
                char car;
                if (d == ClassHelper.int_TYPE) {
                    car = 'I';
                } else if (d == ClassHelper.VOID_TYPE) {
                    car = 'V';
                } else if (d == ClassHelper.boolean_TYPE) {
                    car = 'Z';
                } else if (d == ClassHelper.byte_TYPE) {
                    car = 'B';
                } else if (d == ClassHelper.char_TYPE) {
                    car = 'C';
                } else if (d == ClassHelper.short_TYPE) {
                    car = 'S';
                } else if (d == ClassHelper.double_TYPE) {
                    car = 'D';
                } else if (d == ClassHelper.float_TYPE) {
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

    protected void pushConstant(boolean value) {
        if (value) {
            mv.visitInsn(Opcodes.ICONST_1);
        } else {
            mv.visitInsn(Opcodes.ICONST_0);
        }
    }

    public void pushConstant(int value) {
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

    public void doCast(Class type) {
        if (type != Object.class) {
            if (type.isPrimitive() && type != Void.TYPE) {
                unbox(type);
            } else {
                mv.visitTypeInsn(
                        Opcodes.CHECKCAST,
                        type.isArray() ? getTypeDescription(type) : getClassInternalName(type.getName()));
            }
        }
    }

    public void doCast(ClassNode type) {
        if (type == ClassHelper.OBJECT_TYPE) return;
        if (ClassHelper.isPrimitiveType(type) && type != ClassHelper.VOID_TYPE) {
            unbox(type);
        } else {
            mv.visitTypeInsn(
                    Opcodes.CHECKCAST,
                    type.isArray() ? getTypeDescription(type) : getClassInternalName(type));
        }
    }

    public void load(ClassNode type, int idx) {
        if (type == ClassHelper.double_TYPE) {
            mv.visitVarInsn(Opcodes.DLOAD, idx);
        } else if (type == ClassHelper.float_TYPE) {
            mv.visitVarInsn(Opcodes.FLOAD, idx);
        } else if (type == ClassHelper.long_TYPE) {
            mv.visitVarInsn(Opcodes.LLOAD, idx);
        } else if (
                type == ClassHelper.boolean_TYPE
                        || type == ClassHelper.char_TYPE
                        || type == ClassHelper.byte_TYPE
                        || type == ClassHelper.int_TYPE
                        || type == ClassHelper.short_TYPE) {
            mv.visitVarInsn(Opcodes.ILOAD, idx);
        } else {
            mv.visitVarInsn(Opcodes.ALOAD, idx);
        }
    }

    public void load(Variable v) {
        load(v.getType(), v.getIndex());
    }

    public void store(Variable v, boolean markStart) {
        ClassNode type = v.getType();
        int idx = v.getIndex();

        if (type == ClassHelper.double_TYPE) {
            mv.visitVarInsn(Opcodes.DSTORE, idx);
        } else if (type == ClassHelper.float_TYPE) {
            mv.visitVarInsn(Opcodes.FSTORE, idx);
        } else if (type == ClassHelper.long_TYPE) {
            mv.visitVarInsn(Opcodes.LSTORE, idx);
        } else if (
                type == ClassHelper.boolean_TYPE
                        || type == ClassHelper.char_TYPE
                        || type == ClassHelper.byte_TYPE
                        || type == ClassHelper.int_TYPE
                        || type == ClassHelper.short_TYPE) {
            mv.visitVarInsn(Opcodes.ISTORE, idx);
        } else {
            mv.visitVarInsn(Opcodes.ASTORE, idx);
        }
    }

    public void store(Variable v) {
        store(v, false);
    }

    /**
     * load the constant on the operand stack. primitives auto-boxed.
     */
    void loadConstant(Object value) {
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
     */
    public void loadVar(Variable variable) {
        int index = variable.getIndex();
        load(variable);
        box(variable.getType());
    }

    public void storeVar(Variable variable) {
        String type = variable.getTypeName();
        int index = variable.getIndex();
        store(variable, false);
    }

    public void putField(FieldNode fld) {
        putField(fld, getClassInternalName(fld.getOwner()));
    }

    public void putField(FieldNode fld, String ownerName) {
        mv.visitFieldInsn(Opcodes.PUTFIELD, ownerName, fld.getName(), getTypeDescription(fld.getType()));
    }

    public void swapObjectWith(ClassNode type) {
        if (type == ClassHelper.long_TYPE || type == ClassHelper.double_TYPE) {
            mv.visitInsn(Opcodes.DUP_X2);
            mv.visitInsn(Opcodes.POP);
        } else {
            mv.visitInsn(Opcodes.SWAP);
        }
    }

    public void swapWithObject(ClassNode type) {
        if (type == ClassHelper.long_TYPE || type == ClassHelper.double_TYPE) {
            mv.visitInsn(Opcodes.DUP2_X1);
            mv.visitInsn(Opcodes.POP2);
        } else {
            mv.visitInsn(Opcodes.SWAP);
        }
    }

    public static ClassNode boxOnPrimitive(ClassNode type) {
        if (!type.isArray()) return ClassHelper.getWrapper(type);
        return boxOnPrimitive(type.getComponentType()).makeArray();
    }

    /**
     * convert boolean to Boolean
     */
    public void boxBoolean() {
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
     */
    public void negateBoolean() {
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
     */
    public void mark(String msg) {
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

    public void dup(ClassNode type) {
        if (type == ClassHelper.double_TYPE || type == ClassHelper.long_TYPE)
            mv.visitInsn(Opcodes.DUP2);
        else
            mv.visitInsn(Opcodes.DUP);
    }

    public void dup_x1(ClassNode type) {
        if (type == ClassHelper.double_TYPE || type == ClassHelper.long_TYPE)
            mv.visitInsn(Opcodes.DUP2_X1);
        else
            mv.visitInsn(Opcodes.DUP_X1);
    }

    public void dup_x2(ClassNode type) {
        if (type == ClassHelper.double_TYPE || type == ClassHelper.long_TYPE)
            mv.visitInsn(Opcodes.DUP2_X2);
        else
            mv.visitInsn(Opcodes.DUP_X2);
    }

    public void pop(ClassNode type) {
        if (type == ClassHelper.double_TYPE || type == ClassHelper.long_TYPE)
            mv.visitInsn(Opcodes.POP2);
        else
            mv.visitInsn(Opcodes.POP);
    }

    public static void doReturn(MethodVisitor mv, ClassNode returnType) {
        if (returnType == ClassHelper.double_TYPE) {
            mv.visitInsn(Opcodes.DRETURN);
        } else if (returnType == ClassHelper.float_TYPE) {
            mv.visitInsn(Opcodes.FRETURN);
        } else if (returnType == ClassHelper.long_TYPE) {
            mv.visitInsn(Opcodes.LRETURN);
        } else if (
                returnType == ClassHelper.boolean_TYPE
                        || returnType == ClassHelper.char_TYPE
                        || returnType == ClassHelper.byte_TYPE
                        || returnType == ClassHelper.int_TYPE
                        || returnType == ClassHelper.short_TYPE) {
            //byte,short,boolean,int are all IRETURN
            mv.visitInsn(Opcodes.IRETURN);
        } else if (returnType == ClassHelper.VOID_TYPE) {
            mv.visitInsn(Opcodes.RETURN);
        } else {
            mv.visitInsn(Opcodes.ARETURN);
        }

    }

    public void doReturn(ClassNode returnType) {
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
        if (!ClassHelper.isPrimitiveType(printType)) ret.append(";");
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

    public void cast(ClassNode expr, ClassNode type) {
        if (ClassHelper.isPrimitiveType(expr) || ClassHelper.isPrimitiveType(type)) {
            throw new RuntimeException("Can't convert " + expr.getName() + " to " + type.getName());
        }

        if (TypeUtil.isDirectlyAssignableFrom(type, expr)) {
            return;
        }

        if (TypeUtil.isIntegralType(expr)) {
            castIntegral(expr, type);
        }
        else if (expr == ClassHelper.Long_TYPE) {
            castLong(expr, type);
        }
        else if (expr == ClassHelper.Double_TYPE) {
            castDouble(expr, type);
        }
        else if (expr == ClassHelper.Float_TYPE) {
            castFloat(expr, type);
        }
        else if (expr == ClassHelper.BigDecimal_TYPE) {
            castBigDecimal(expr, type);
        }
        else if (expr == ClassHelper.BigInteger_TYPE) {
            castBigInteger(expr, type);
        }
        else {
            if (TypeUtil.isNumericalType(type)) {
                unbox(ClassHelper.getUnwrapper(type));
                box(ClassHelper.getUnwrapper(type));
            }
            else {
                if (type.equals(ClassHelper.STRING_TYPE))
                   mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;"); 
                else
                   mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(type));
            }
        }
    }

    private void castIntegral(ClassNode expr, ClassNode type) {
        if (type == ClassHelper.Integer_TYPE) {
        }
        else if (type == ClassHelper.Boolean_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IAND);
            box(ClassHelper.boolean_TYPE);
        }
        else if (type == ClassHelper.Byte_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(I2B);
            box(ClassHelper.byte_TYPE);
        }
        else if (type == ClassHelper.Short_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(I2S);
            box(ClassHelper.short_TYPE);
        }
        else if (type == ClassHelper.Character_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(I2C);
            box(ClassHelper.char_TYPE);
        }
        else if (type == ClassHelper.Long_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(I2L);
            box(ClassHelper.long_TYPE);
        }
        else if (type == ClassHelper.Float_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(I2F);
            box(ClassHelper.float_TYPE);
        }
        else if (type == ClassHelper.Double_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(I2D);
            box(ClassHelper.double_TYPE);
        }
        else if (type == ClassHelper.BigDecimal_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigDecimal");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigDecimal", "<init>", "(Ljava/lang/String;)V");
        }
        else if (type == ClassHelper.BigInteger_TYPE) {
            if (expr.equals(ClassHelper.Character_TYPE)) {
                mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C");
                box(ClassHelper.int_TYPE);
            }
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigInteger");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigInteger", "<init>", "(Ljava/lang/String;)V");
        }
        else {
            mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(type));
        }
    }

    private void castLong(ClassNode expr, ClassNode type) {
        if (type == ClassHelper.Integer_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(L2I);
            box(ClassHelper.int_TYPE);
        }
        else if (type == ClassHelper.Boolean_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(L2I);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IAND);
            box(ClassHelper.boolean_TYPE);
        }
        else if (type == ClassHelper.Byte_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(L2I);
            mv.visitInsn(I2B);
            box(ClassHelper.byte_TYPE);
        }
        else if (type == ClassHelper.Short_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(L2I);
            mv.visitInsn(I2S);
            box(ClassHelper.short_TYPE);
        }
        else if (type == ClassHelper.Character_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(L2I);
            mv.visitInsn(I2C);
            box(ClassHelper.char_TYPE);
        }
        else if (type == ClassHelper.Long_TYPE) {
        }
        else if (type == ClassHelper.Float_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(L2F);
            box(ClassHelper.float_TYPE);
        }
        else if (type == ClassHelper.Double_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(L2D);
            box(ClassHelper.double_TYPE);
        }
        else if (type == ClassHelper.BigDecimal_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigDecimal");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigDecimal", "<init>", "(Ljava/lang/String;)V");
        }
        else if (type == ClassHelper.BigInteger_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigInteger");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigInteger", "<init>", "(Ljava/lang/String;)V");
        }
        else {
            mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(type));
        }
    }

    private void castDouble(ClassNode expr, ClassNode type) {
        if (type == ClassHelper.Integer_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(D2I);
            box(ClassHelper.int_TYPE);
        }
        else if (type == ClassHelper.Boolean_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(D2I);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IAND);
            box(ClassHelper.boolean_TYPE);
        }
        else if (type == ClassHelper.Byte_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(D2I);
            mv.visitInsn(I2B);
            box(ClassHelper.byte_TYPE);
        }
        else if (type == ClassHelper.Short_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(D2I);
            mv.visitInsn(I2S);
            box(ClassHelper.short_TYPE);
        }
        else if (type == ClassHelper.Character_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(D2I);
            mv.visitInsn(I2C);
            box(ClassHelper.char_TYPE);
        }
        else if (type == ClassHelper.Long_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(D2L);
            box(ClassHelper.long_TYPE);
        }
        else if (type == ClassHelper.Float_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(D2F);
            box(ClassHelper.float_TYPE);
        }
        else if (type == ClassHelper.Double_TYPE) {
        }
        else if (type == ClassHelper.BigDecimal_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigDecimal");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigDecimal", "<init>", "(Ljava/lang/String;)V");
        }
        else if (type == ClassHelper.BigInteger_TYPE) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J");
            box(ClassHelper.long_TYPE);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigInteger");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigInteger", "<init>", "(Ljava/lang/String;)V");
        }
        else {
            mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(type));
        }
    }

    private void castFloat(ClassNode expr, ClassNode type) {
        if (type == ClassHelper.Integer_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(F2I);
            box(ClassHelper.int_TYPE);
        }
        else if (type == ClassHelper.Boolean_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(F2I);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IAND);
            box(ClassHelper.boolean_TYPE);
        }
        else if (type == ClassHelper.Byte_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(F2I);
            mv.visitInsn(I2B);
            box(ClassHelper.byte_TYPE);
        }
        else if (type == ClassHelper.Short_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(F2I);
            mv.visitInsn(I2S);
            box(ClassHelper.short_TYPE);
        }
        else if (type == ClassHelper.Character_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(F2I);
            mv.visitInsn(I2C);
            box(ClassHelper.char_TYPE);
        }
        else if (type == ClassHelper.Long_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(F2L);
            box(ClassHelper.long_TYPE);
        }
        else if (type == ClassHelper.Float_TYPE) {
        }
        else if (type == ClassHelper.Double_TYPE) {
            unbox(ClassHelper.getUnwrapper(expr));
            mv.visitInsn(F2D);
            box(ClassHelper.double_TYPE);
        }
        else if (type == ClassHelper.BigDecimal_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigDecimal");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigDecimal", "<init>", "(Ljava/lang/String;)V");
        }
        else if (type == ClassHelper.BigInteger_TYPE) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J");
            box(ClassHelper.long_TYPE);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigInteger");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigInteger", "<init>", "(Ljava/lang/String;)V");
        }
        else {
            mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(type));
        }
    }

    private void castBigDecimal(ClassNode expr, ClassNode type) {
        if (type == ClassHelper.Integer_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            box (ClassHelper.int_TYPE);
        }
        else if (type == ClassHelper.Boolean_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IAND);
            box(ClassHelper.boolean_TYPE);
        }
        else if (type == ClassHelper.Byte_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            mv.visitInsn(I2B);
            box(ClassHelper.byte_TYPE);
        }
        else if (type == ClassHelper.Short_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            mv.visitInsn(I2S);
            box(ClassHelper.short_TYPE);
        }
        else if (type == ClassHelper.Character_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            mv.visitInsn(I2C);
            box(ClassHelper.char_TYPE);
        }
        else if (type == ClassHelper.Long_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J");
            box(ClassHelper.long_TYPE);
        }
        else if (type == ClassHelper.Float_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F");
            box(ClassHelper.float_TYPE);
        }
        else if (type == ClassHelper.Double_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D");
            box(ClassHelper.double_TYPE);
        }
        else if (type == ClassHelper.BigDecimal_TYPE) {
        }
        else if (type == ClassHelper.BigInteger_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/math/BigDecimal", "toBigInteger", "()Ljava/math/BigInteger;");
        }
        else {
            mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(type));
        }
    }

    private void castBigInteger(ClassNode expr, ClassNode type) {
        if (type == ClassHelper.Integer_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            box (ClassHelper.int_TYPE);
        }
        else if (type == ClassHelper.Boolean_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IAND);
            box(ClassHelper.boolean_TYPE);
        }
        else if (type == ClassHelper.Byte_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            mv.visitInsn(I2B);
            box(ClassHelper.byte_TYPE);
        }
        else if (type == ClassHelper.Short_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            mv.visitInsn(I2S);
            box(ClassHelper.short_TYPE);
        }
        else if (type == ClassHelper.Character_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
            mv.visitInsn(I2C);
            box(ClassHelper.char_TYPE);
        }
        else if (type == ClassHelper.Long_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J");
            box(ClassHelper.long_TYPE);
        }
        else if (type == ClassHelper.Float_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F");
            box(ClassHelper.float_TYPE);
        }
        else if (type == ClassHelper.Double_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D");
            box(ClassHelper.double_TYPE);
        }
        else if (type == ClassHelper.BigDecimal_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
            mv.visitTypeInsn(NEW, "java/math/BigDecimal");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/math/BigDecimal", "<init>", "(Ljava/lang/String;)V");
        }
        else if (type == ClassHelper.BigInteger_TYPE) {
        }
        else {
            mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(type));
        }
    }

    protected void incOrDecPrimitive(ClassNode primType, final int op) {
        boolean add = op == Types.PLUS_PLUS;
        if (primType == ClassHelper.BigDecimal_TYPE || primType == ClassHelper.BigInteger_TYPE) {
            if (add)
                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/DefaultGroovyMethods", "next", "(Ljava/lang/Number;)Ljava/lang/Number;");
            else
                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/DefaultGroovyMethods", "previous", "(Ljava/lang/Number;)Ljava/lang/Number;");
        }
        else if (primType == ClassHelper.double_TYPE) {
            mv.visitInsn(DCONST_1);
            mv.visitInsn(add ? DADD : DSUB);
        }
        else if (primType == ClassHelper.long_TYPE) {
            mv.visitInsn(LCONST_1);
            mv.visitInsn(add ? LADD : LSUB);
        }
        else if (primType == ClassHelper.float_TYPE) {
            mv.visitInsn(FCONST_1);
            mv.visitInsn(add ? FADD : FSUB);
        }
        else {
            mv.visitInsn(ICONST_1);
            mv.visitInsn(add ? IADD : ISUB);
        }
    }

    protected void toInt(ClassNode type) {
        if (ClassHelper.isPrimitiveType(type)) {
            if (type == ClassHelper.double_TYPE) {
                mv.visitInsn(D2I);
            }
            else if (type == ClassHelper.long_TYPE) {
                mv.visitInsn(L2I);
            }
            else if (type == ClassHelper.float_TYPE) {
                mv.visitInsn(F2I);
            }
        }
        else {
            unbox(ClassHelper.int_TYPE);
        }
    }

    protected void loadArray(ClassNode type) {
        if (type == ClassHelper.byte_TYPE) {
            mv.visitInsn(BALOAD);
        }
        else if (type == ClassHelper.char_TYPE) {
            mv.visitInsn(CALOAD);
        }
        else if (type == ClassHelper.short_TYPE) {
            mv.visitInsn(SALOAD);
        }
        else if (type == ClassHelper.int_TYPE) {
            mv.visitInsn(IALOAD);
        }
        else if (type == ClassHelper.long_TYPE) {
            mv.visitInsn(LALOAD);
        }
        else if (type == ClassHelper.float_TYPE) {
            mv.visitInsn(FALOAD);
        }
        else if (type == ClassHelper.double_TYPE) {
            mv.visitInsn(DALOAD);
        }
        else {
            mv.visitInsn(AALOAD);
        }
    }

    protected void storeArray(ClassNode type) {
        if (type == ClassHelper.byte_TYPE) {
            mv.visitInsn(BASTORE);
        }
        else if (type == ClassHelper.char_TYPE) {
            mv.visitInsn(CASTORE);
        }
        else if (type == ClassHelper.short_TYPE) {
            mv.visitInsn(SASTORE);
        }
        else if (type == ClassHelper.int_TYPE) {
            mv.visitInsn(IASTORE);
        }
        else if (type == ClassHelper.long_TYPE) {
            mv.visitInsn(LASTORE);
        }
        else if (type == ClassHelper.float_TYPE) {
            mv.visitInsn(FASTORE);
        }
        else if (type == ClassHelper.double_TYPE) {
            mv.visitInsn(DASTORE);
        }
        else {
            mv.visitInsn(AASTORE);
        }
    }
}
