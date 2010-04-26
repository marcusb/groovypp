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

package org.mbte.groovypp.compiler.Issues

import groovy.lang.GroovyClassLoader

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.CompileUnit
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.control.CompilerConfiguration

import org.junit.Test

import org.mbte.groovypp.compiler.AsyncASTTransform

class Issue231Test
{
    // star imports must end with dot-star, ensure the dot is present
    @Test
    void starImportsEndWithDot()
    {
        def t = new AsyncASTTransform()
        def module = new ModuleNode(new CompileUnit(new GroovyClassLoader(), new CompilerConfiguration()))
        t.visit([module] as ASTNode[], null)
        assert module.starImports
        module.starImports.each {
            assert it.packageName.endsWith('.')
        }
    }
}
