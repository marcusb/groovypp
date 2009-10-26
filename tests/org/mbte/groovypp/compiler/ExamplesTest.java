package org.mbte.groovypp.compiler;

import junit.framework.Test;
import junit.framework.TestCase;
import groovy.util.AllTestSuite;
import groovy.lang.GroovyClassLoader;

public class ExamplesTest extends TestCase {
    public static Test suite () {
        return AllTestSuite.suite("./Examples/", "**/*.groovy");
    }
}
