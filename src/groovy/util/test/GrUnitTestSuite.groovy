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