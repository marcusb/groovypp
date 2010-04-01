package groovy.util.test

import junit.framework.TestSuite
import junit.framework.Test

@Typed class GrUnitTestSuite extends TestSuite {
    public static final String SYSPROP_TEST_DIR = "groovy.test.dir";

    static Test suite() {
        String basedir = new File(System.getProperty(SYSPROP_TEST_DIR, "./test/")).absolutePath

        GrUnitTestSuite suite = []

        List<String> names = new FileNameFinder().getFileNames(basedir, "**/*\$GrUnitTest.class")
        for (fileName in names) {
            String name = fileName.substring(basedir.length() + 1).replace("/", ".")
            name = name.substring(0, name.length()-".class".length())
            println name
            Class testClass = Class.forName(name)
            suite.addTestSuite(testClass)
        }

        suite
    }
}