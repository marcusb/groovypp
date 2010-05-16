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

package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.runtime.DefaultGroovyStaticMethods;
import org.codehaus.groovy.util.FastArray;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.vmplugin.v5.PluginDefaultGroovyMethods;
import org.mbte.groovypp.runtime.DefaultGroovyPPMethods;
import org.mbte.groovypp.runtime.ArraysMethods;
import org.mbte.groovypp.runtime.DefaultGroovyPPStaticMethods;
import org.objectweb.asm.Opcodes;

import java.lang.ref.SoftReference;
import java.util.*;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class ClassNodeCache {
    public static DGM createDGM(MethodNode method) {
        final Parameter[] pp = method.getParameters();
        Parameter params[] = pp.length > 1 ? new Parameter[pp.length - 1] : Parameter.EMPTY_ARRAY;
        for (int j = 0; j != params.length; ++j)
            params[j] = new Parameter(pp[j + 1].getType(), "$" + j);

        DGM mn = new DGM(
                method.getName(),
                Opcodes.ACC_PUBLIC,
                method.getReturnType(),
                params,
                method.getExceptions(),
                null);
        mn.setDeclaringClass(pp[0].getType());
        mn.callClassInternalName = BytecodeHelper.getClassInternalName(method.getDeclaringClass());
        mn.descr = BytecodeHelper.getMethodDescriptor(method.getReturnType(), method.getParameters());
        mn.setGenericsTypes(method.getGenericsTypes());
        mn.original = method;
        return mn;
    }

    public static class ClassNodeInfo {
        Map<String, Object> methods;
        Map<String, Object> fields;
        Map<String, Object> staticMethods;
        public FastArray constructors;

        List<MethodNode> isOneMethodAbstract;
    }

    public static class CompileUnitInfo extends HashMap<ClassNode, ClassNodeInfo> {
    }

    static final WeakHashMap<Class, SoftReference<ClassNodeInfo>> loadedClassesCache = new WeakHashMap<Class, SoftReference<ClassNodeInfo>>();

    static final WeakHashMap<CompileUnit, SoftReference<CompileUnitInfo>> compiledClassesCache = new WeakHashMap<CompileUnit, SoftReference<CompileUnitInfo>>();

    static final Map<ClassNode, List<MethodNode>> dgmMethods = new HashMap<ClassNode, List<MethodNode>>();

    static {
        initDgm(DefaultGroovyPPMethods.class, false);
        initDgm(DefaultGroovyPPStaticMethods.class, true);
        initDgm(DefaultGroovyStaticMethods.class, true);
        initDgm(ArraysMethods.class, false);
        addGlobalDGM();
        initDgm(Arrays.class, false);
        initDgm(Collections.class, new HashSet<String>(Arrays.asList(
                "void sort(java.util.List)",
                "void sort(java.util.List, java.util.Comparator)")), false);
        initDgm(PluginDefaultGroovyMethods.class,false);
        initDgm(DefaultGroovyMethods.class, new HashSet<String>(Arrays.asList(
                "java.lang.Object each(java.lang.Object, groovy.lang.Closure)",
                "java.util.Iterator each(java.util.Iterator, groovy.lang.Closure)",
                "java.util.Map each(java.util.Map, groovy.lang.Closure)",
                "boolean any(java.lang.Object, groovy.lang.Closure)",
                "boolean any(java.util.Iterator, groovy.lang.Closure)",
                "boolean any(java.util.Map, groovy.lang.Closure)",
                "java.lang.Object find(java.lang.Object, groovy.lang.Closure)",
                "java.lang.Object find(java.util.Iterator, groovy.lang.Closure)",
                "java.util.Map.Entry find(java.util.Map, groovy.lang.Closure)",
                "java.lang.Object getAt(java.lang.Object, java.lang.String)",
                "void putAt(java.lang.Object, java.lang.String, java.lang.Object)",
                "java.util.Iterator iterator(java.lang.Object)",
                "void eachFile(java.io.File, groovy.lang.Closure)",
                "void eachDir(java.io.File, groovy.lang.Closure)",
                "void eachFileRecurse(java.io.File, groovy.lang.Closure)",
                "void eachDirRecurse(java.io.File, groovy.lang.Closure)",
                "java.lang.String eachMatch(java.lang.String, java.lang.String, groovy.lang.Closure)",
                "java.lang.String eachMatch(java.lang.String, java.util.regex.Pattern, groovy.lang.Closure)",
                "java.util.Map sort(java.util.Map, groovy.lang.Closure)",
                "java.util.Map sort(java.util.Map, java.util.Comparator)",
                "java.util.List sort(java.util.Collection, groovy.lang.Closure)",
                "java.lang.Object newInstance(java.lang.Class)",
                "java.lang.Object with(java.lang.Object, groovy.lang.Closure)",
                "java.util.List collect(java.lang.Object, groovy.lang.Closure)",
                "java.util.List collect(java.lang.Object, java.util.Collection, groovy.lang.Closure)",
                "java.util.List collect(java.util.Collection, groovy.lang.Closure)",
                "java.util.Collection collect(java.util.Collection, java.util.Collection, groovy.lang.Closure)",
                "java.util.Collection collect(java.util.Map, groovy.lang.Closure)",
                "java.util.Collection collect(java.util.Map, java.util.Collection, groovy.lang.Closure)",
                "java.lang.String toString(java.lang.Object)",
                "void print(java.lang.Object, java.lang.Object)",
                "void println(java.lang.Object, java.lang.Object)",
                "java.lang.Object withReader(java.io.File, groovy.lang.Closure)",
                "java.lang.Object withReader(java.io.File, java.lang.String, groovy.lang.Closure)",
                "java.lang.Object withReader(java.io.Reader, groovy.lang.Closure)",
                "java.lang.Object withWriter(java.io.File, groovy.lang.Closure)",
                "java.lang.Object withWriter(java.io.File, java.lang.String, groovy.lang.Closure)",
                "java.lang.Object withWriter(java.io.Reader, groovy.lang.Closure)",
                "java.lang.Object withOutputStream(java.io.File, groovy.lang.Closure)",
                "java.lang.Object withInputStream(java.io.File, groovy.lang.Closure)",
                "java.lang.Object withDataOutputStream(java.io.File, groovy.lang.Closure)",
                "java.lang.Object withDataInputStream(java.io.File, groovy.lang.Closure)",
                "java.lang.Object withInputStream(java.net.URL, groovy.lang.Closure)",
                "java.lang.Object withStream(java.io.InputStream, groovy.lang.Closure)",
                "java.lang.Object withStream(java.io.OutputStream, groovy.lang.Closure)",
                "java.util.Collection flatten(java.util.Collection)"
        )), false);
    }

    private static void initDgm(String klazz) {
        try {
            initDgm(Class.forName(klazz), false);
        } catch (ClassNotFoundException e) { //
            System.err.println("failed to load " + klazz);
        }
    }

    static ClassNodeInfo getClassNodeInfo(ClassNode classNode) {
        final ModuleNode moduleNode;
        ClassNode cn = classNode;
        while (cn.isArray())
            cn = cn.getComponentType();
        moduleNode = cn.getModule();

        if (moduleNode != null) {
            final CompileUnit compileUnit = classNode.getCompileUnit();
            final SoftReference<CompileUnitInfo> ref = compiledClassesCache.get(compileUnit);
            CompileUnitInfo cui;
            if (ref == null || (cui = ref.get()) == null) {
                cui = new CompileUnitInfo();
                compiledClassesCache.put(compileUnit, new SoftReference<CompileUnitInfo>(cui));
            }

            ClassNodeInfo info = cui.get(classNode);
            if (info == null) {
                info = new ClassNodeInfo();
                cui.put(classNode, info);
            }
            return info;
        } else {
            Class typeClass = classNode.getTypeClass();
            final SoftReference<ClassNodeInfo> ref = loadedClassesCache.get(typeClass);
            ClassNodeInfo cni;
            if (ref == null || (cni = ref.get()) == null) {
                cni = new ClassNodeInfo();
                loadedClassesCache.put(typeClass, new SoftReference<ClassNodeInfo>(cni));
            }
            return cni;
        }
    }

    static synchronized FastArray getConstructors(ClassNode type) {
        final ClassNodeInfo info = getClassNodeInfo(type);

        FastArray list = info.constructors;
        if (list == null) {
            list = new FastArray();
            info.constructors = list;

            List constructors = type.redirect().getDeclaredConstructors();
            if (constructors.isEmpty()) {
                ConstructorNode constructorNode = new ConstructorNode(Opcodes.ACC_PUBLIC, null);
                constructorNode.setSynthetic(true);
                type.addConstructor(constructorNode);
                list.add(constructorNode);
            }
            else
                for (Object o : constructors) {
                    ConstructorNode cn = (ConstructorNode) o;
                    list.add(cn);
                }
        }
        return list;
    }

    public static void clearCache(ClassNode classNode) {
        final ModuleNode moduleNode;
        ClassNode cn = classNode;
        while (cn.isArray())
            cn = cn.getComponentType();
        moduleNode = cn.getModule();

        if (moduleNode != null) {
            final CompileUnit compileUnit = classNode.getCompileUnit();
            final SoftReference<CompileUnitInfo> ref = compiledClassesCache.get(compileUnit);
            CompileUnitInfo cui;
            if (ref == null || (cui = ref.get()) == null) {
                cui = new CompileUnitInfo();
                compiledClassesCache.put(compileUnit, new SoftReference<CompileUnitInfo>(cui));
            }

            cui.remove(classNode);
        }
    }

    public static synchronized Object getMethods(ClassNode type, String methodName) {

        final ClassNodeInfo info = getClassNodeInfo(type.redirect());

        if (info.methods == null) {
            fillMethodsMaps(type, info);
        }
        return info.methods.get(methodName);
    }

    public static synchronized Object getStaticMethods(ClassNode type, String methodName) {

        final ClassNodeInfo info = getClassNodeInfo(type.redirect());

        if (info.staticMethods == null) {
            fillMethodsMaps(type, info);
        }
        return info.staticMethods.get(methodName);
    }

    private static void fillMethodsMaps(ClassNode type, ClassNodeInfo info) {
        Map<String, Object> methodsMap = new HashMap<String, Object>();
        Map<String, Object> staticMethodsMap = new HashMap<String, Object>();

        final Set<ClassNode> ifaces = getAllInterfaces(type);
        for (ClassNode node : ifaces) {
            addMethods(methodsMap, staticMethodsMap, node.getMethods(), true);
        }

        for (ClassNode cn : getSuperClassesAndSelf(type)) {
            addMethods(methodsMap, staticMethodsMap, cn.getMethods(), cn == type);

            final List<MethodNode> list = dgmMethods.get(cn);
            if (list != null) {
                addMethods(methodsMap, staticMethodsMap, list, true);
            }
        }

        for (ClassNode node : ifaces) {
            final List<MethodNode> list = dgmMethods.get(node);
            if (list != null) {
                addMethods(methodsMap, staticMethodsMap, list, true);
            }
        }

        if (type.isArray()) {
            final MethodNode cloneNode = new MethodNode("clone", Opcodes.ACC_PUBLIC, ClassHelper.OBJECT_TYPE, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, null);
            cloneNode.setDeclaringClass(type);
            addMethods(methodsMap, staticMethodsMap, Collections.singletonList(cloneNode), true);
        }
        info.methods = methodsMap;
        info.staticMethods = staticMethodsMap;
    }

    private static List<ClassNode> getSuperClassesAndSelf(ClassNode classNode) {
        if (classNode.isInterface()) {
            return Arrays.asList(classNode, ClassHelper.OBJECT_TYPE);
        } else {
            List<ClassNode> superClasses = new ArrayList<ClassNode>();
            superClasses.add(classNode);
            getSuperClasses(classNode, superClasses);
            return superClasses;
        }
    }

    private static void getSuperClasses(ClassNode classNode, List<ClassNode> superClasses) {
        if (classNode.isArray()) {
            ClassNode componentType = classNode.getComponentType();
            if (!ClassHelper.isPrimitiveType(componentType)) {
                ClassNode aSuper = componentType.getSuperClass();
                if (aSuper != null) {
                    ClassNode superArray = aSuper.makeArray();
                    superClasses.add(superArray);
                    getSuperClasses(superArray, superClasses);
                    return;
                }
            }
            superClasses.add(ClassHelper.OBJECT_TYPE);
        } else {
            ClassNode aSuper;
            if (ClassHelper.isPrimitiveType(classNode)) {
                aSuper = ClassHelper.getWrapper(classNode);
            }
            else {
                aSuper = classNode.getSuperClass();
            }
            if (aSuper != null) {
                superClasses.add(aSuper);
                getSuperClasses(aSuper, superClasses);
            }
        }
    }

    static Object getFields(ClassNode type, String fieldName) {
        final ClassNodeInfo info = getClassNodeInfo(type);

        Map<String, Object> nameMap = info.fields;
        if (nameMap == null) {
            nameMap = new HashMap<String, Object>();

            addFields(nameMap, type, type);
            info.fields = nameMap;
        }
        return nameMap.get(fieldName);
    }

    private static void addFields(Map<String, Object> nameMap, ClassNode type, ClassNode placeClass) {
        ClassNode superClass = type.getSuperClass();
        if (superClass != null) addFields(nameMap, superClass, placeClass);
        ClassNode[] ifaces = type.getInterfaces();
        if (ifaces != null) {
            for (ClassNode iface : ifaces) {
                addFields(nameMap, iface, placeClass);
            }
        }

        for (FieldNode m : type.getFields()) {
            // Not 100% correct but will do for now.
            nameMap.put(m.getName(), m);
        }
    }

    static void addMethods(Map<String, Object> nameMap,
                           Map<String, Object> staticMethodsMap,
                           List<MethodNode> methods,
                           boolean usePrivate) {
        for (MethodNode m : methods) {
            if ((m.getModifiers() & Opcodes.ACC_BRIDGE) == 0) {
                if (usePrivate || !m.isPrivate()) {
                    nameMap.put(m.getName(), addMethodToList(nameMap.get(m.getName()), m));
                    if (m.isStatic()) {
                        staticMethodsMap.put(m.getName(), addMethodToList(staticMethodsMap.get(m.getName()), m));
                        if (m.getParameters().length > 0) {
                            if (m.getParameters()[0].getType().equals(m.getDeclaringClass())) {
                                nameMap.put(m.getName(), addMethodToList(nameMap.get(m.getName()), createDGM(m)));
                            }
                        }
                    }
                }
            }
        }
    }

    static void getAllInterfaces(ClassNode type, Set<ClassNode> res) {
        if (type == null)
            return;

        ClassNode[] interfaces = type.getInterfaces();
        for (ClassNode anInterface : interfaces) {
            res.add(anInterface);
            getAllInterfaces(anInterface, res);
        }

        if (ClassHelper.isPrimitiveType(type))
            getAllInterfaces(ClassHelper.getWrapper(type), res);
        else
            getAllInterfaces(type.getSuperClass(), res);
    }

    static Set<ClassNode> getAllInterfaces(ClassNode type) {
        Set<ClassNode> res = new HashSet<ClassNode>();
        getAllInterfaces(type, res);
        return res;
    }

    public static Object addMethodToList(Object o, MethodNode method) {
        if (o == null) {
            return method;
        }

        if (o instanceof MethodNode) {
            MethodNode match = (MethodNode) o;
            if (!isMatchingMethod(match, method)) {
                FastArray list = new FastArray(2);
                list.add(match);
                list.add(method);
                return list;
            } else {
                if (match.isPrivate()
                        || (!isNonRealMethod(match) && match.getDeclaringClass().isInterface() && !method.getDeclaringClass().isInterface())) {
                    // do not overwrite interface methods with instance methods
                    // do not overwrite private methods
                    // Note: private methods from parent classes are not shown here,
                    // but when doing the multimethod connection step, we overwrite
                    // methods of the parent class with methods of a subclass and
                    // in that case we want to keep the private methods
                } else {
                    ClassNode methodC = method.getDeclaringClass();
                    ClassNode matchC = match.getDeclaringClass();
                    if (methodC.equals(matchC)) {
                        if (isNonRealMethod(method)) {
                            return method;
                        }
                    } else if (!TypeUtil.isDirectlyAssignableFrom(methodC, matchC) && !ClassHelper.isPrimitiveType(matchC)) {
                        return method;
                    }
                }
            }
            return o;
        }

        if (o instanceof FastArray) {
            FastArray list = (FastArray) o;
            int found = findMatchingMethod(list, method);

            if (found == -1) {
                list.add(method);
            } else {
                MethodNode match = (MethodNode) list.get(found);
                if (match.equals(method)) return o;
                if (match.isPrivate()
                        || (!isNonRealMethod(match) && match.getDeclaringClass().isInterface() && !method.getDeclaringClass().isInterface())) {
                    // do not overwrite interface methods with instance methods
                    // do not overwrite private methods
                    // Note: private methods from parent classes are not shown here,
                    // but when doing the multimethod connection step, we overwrite
                    // methods of the parent class with methods of a subclass and
                    // in that case we want to keep the private methods
                } else {
                    ClassNode methodC = method.getDeclaringClass();
                    ClassNode matchC = match.getDeclaringClass();
                    if (methodC.equals(matchC)) {
                        if (isNonRealMethod(method)) {
                            list.set(found, method);
                        }
                    } else if (!TypeUtil.isDirectlyAssignableFrom(methodC, matchC) && !ClassHelper.isPrimitiveType(matchC)) {
                        list.set(found, method);
                    }
                }
            }
        }

        return o;
    }

    private static boolean isNonRealMethod(MethodNode method) {
        return method instanceof DGM;
    }

    private static boolean isMatchingMethod(MethodNode aMethod, MethodNode method) {
        if (aMethod.equals(method)) return true;

        Parameter[] params1 = aMethod.getParameters();
        Parameter[] params2 = method.getParameters();
        if (params1.length != params2.length) return false;

        for (int i = 0; i < params1.length; i++) {
            if (!params1[i].getType().equals(params2[i].getType())) return false;
        }
        return true;
    }

    private static int findMatchingMethod(FastArray list, MethodNode method) {
        int len = list.size();
        Object data[] = list.getArray();
        for (int j = 0; j != len; ++j) {
            MethodNode aMethod = (MethodNode) data[j];
            if (isMatchingMethod(aMethod, method))
                return j;
        }
        return -1;
    }

    private static void initDgm(final Class klazz, boolean isStatic) {
        initDgm(klazz, Collections.<String>emptySet(), isStatic);
    }

    private static void initDgm(final Class klazz, Set<String> ignore, boolean isStatic) {
        ClassNode classNode = ClassHelper.make(klazz);
        List<MethodNode> methodList = classNode.getMethods();

        for (MethodNode methodNode : methodList) {
            Parameter[] parameters = methodNode.getParameters();
            if (methodNode.isPublic() && methodNode.isStatic() && parameters.length > 0) {
                if (ignore.contains(methodNode.getTypeDescriptor()))
                    continue;
                
                ClassNode declaringClass = methodNode.getParameters()[0].getType();

                Parameter params[] = parameters.length > 1 ? new Parameter[parameters.length - 1] : Parameter.EMPTY_ARRAY;
                for (int j = 0; j != params.length; ++j)
                    params[j] = parameters[j+1];

                DGM mn = createDGM(klazz, methodNode, declaringClass, methodNode.getExceptions(), params, isStatic);

                List<MethodNode> list = dgmMethods.get(declaringClass);
                if (list == null) {
                    list = new ArrayList<MethodNode>(4);
                    dgmMethods.put(declaringClass, list);
                }
                list.add(mn);
            }
        }
    }

    private static DGM createDGM(Class klazz, MethodNode method, ClassNode declaringClass, ClassNode[] exs, Parameter[] params, boolean isStatic) {
        DGM mn = new DGM(
                method.getName(),
                Opcodes.ACC_PUBLIC | (isStatic ? Opcodes.ACC_STATIC : 0),
                method.getReturnType(),
                params,
                exs,
                null);
        mn.setDeclaringClass(declaringClass);
        mn.callClassInternalName = BytecodeHelper.getClassInternalName(klazz);
        mn.descr = BytecodeHelper.getMethodDescriptor(method.getReturnType(), method.getParameters());
        mn.setGenericsTypes(method.getGenericsTypes());
        mn.original = method;
        return mn;
    }

    public static class DGM extends MethodNode {
        public String descr;
        public String callClassInternalName;

        public MethodNode original;

        public DGM(String name, int modifiers, ClassNode returnType, Parameter[] parameters, ClassNode[] exceptions, Statement code) {
            super(name, modifiers, returnType, parameters, exceptions, code);
        }
    }

    private static void addGlobalDGM() {
        Map<String, URL> names = new LinkedHashMap<String, URL>();
        try {
            Enumeration<URL> globalServices = ClassNodeCache.class.getClassLoader().getResources("META-INF/services/org.mbte.groovypp.compiler.Extensions");
            while (globalServices.hasMoreElements()) {
                URL service = globalServices.nextElement();
                String className;
                BufferedReader svcIn = new BufferedReader(new InputStreamReader(service.openStream()));
                try {
                    className = svcIn.readLine();
                } catch (IOException ioe) {
                    continue;
                }
                while (className != null) {
                    if (!className.startsWith("#") && className.length() > 0) {
                        names.put(className, service);
                    }
                    try {
                        className = svcIn.readLine();
                    } catch (IOException ioe) {//
                    }
                }
            }
        } catch (IOException e) { //
        }

        for (String name : names.keySet()) {
            initDgm(name);
        }
    }
}
