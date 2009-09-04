package org.mbte.groovypp.compiler;

import groovy.util.GroovyTestCase;
import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.io.IOException;

public class GPTest extends GroovyTestCase {
    public void testGP () throws IOException {
        final GroovyClassLoader loader = new GroovyClassLoader(GPTest.class.getClassLoader());
        loader.parseClass(new File("/Development/GParallelizer/src/main/groovy/org/gparallelizer/actors/AbstractThreadActor.groovy"));
    }
}
