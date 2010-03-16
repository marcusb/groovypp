package org.mbte.groovypp.compiler;

import org.codehaus.groovy.reflection.CachedField;
import org.codehaus.groovy.reflection.ReflectionCache;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

class DebugMethodAdapter{

    private static Map<Integer,String> map = new HashMap<Integer,String>();

    static {
        final CachedField[] cachedFields = ReflectionCache.getCachedClass(Opcodes.class).getFields();
        for (int i = 0; i < cachedFields.length; i++) {
            CachedField cachedField = cachedFields[i];
            final Object v = cachedField.getProperty(null);
            if (v instanceof Integer)
                map.put ((Integer) v, cachedField.getName());
        }
    }

    static MethodVisitor create(final MethodVisitor mv, final int debug) {
        final Set<String> codes = new HashSet<String>();
        codes.addAll(Arrays.asList("visitVarInsn", "visitMethodInsn", "visitInsn", "visitJumpInsn", "visitTypeInsn", "visitFieldInsn", "visitIntInsn"));
        final char[] debugTab = new char[debug];
        Arrays.fill(debugTab, '\t');
        final String tab = String.valueOf(debugTab);
        return (MethodVisitor) Proxy.newProxyInstance(DebugMethodAdapter.class.getClassLoader(), new Class[]{MethodVisitor.class}, new InvocationHandler(){
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getDeclaringClass() != Class.class) {
                    DebugContext.outputStream.print(tab + method.getName() + "(");
                    for (int i = 0; i < args.length; i++) {
                        if (i == 0 && codes.contains(method.getName())) {
                            DebugContext.outputStream.print(map.get((Integer)args[i]) + ", ");
                        }
                        else {
                          if (i == 0 && method.getName().equals("visitLdcInsn"))
                             DebugContext.outputStream.print("(" + args[i].getClass().getName() + ")" + args[i] + ", ");
                          else
                             DebugContext.outputStream.print(args[i] + ", ");
                        }
                    }
                    DebugContext.outputStream.println(")");
                }
                return method.invoke(mv, args);
            }
        });
    }
}
