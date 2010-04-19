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

package groovy.lang;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Struct is marker annotation for special type of classes with following properties
 * - super class is either Object or also Struct
 * - the class has protected no-arg constructor, so it can't be instantiated directly
 *     - if such constructor does not exist a compile time error will happen
 *     - any other constructor will lead to compile time error as well
 * - the only way to instantiate struct is via inner Builder class
 *     - each Struct has inner class Builder
 *     - Builder has all the same properties as it's outer class but mutable
 * - there are no any methods except getters
 * - properties of Struct are one of the following
 *     - classes implementing Struct
 *     - primitive types (including boxed versions)
 *     - strings
 *     - uiids
 *     - immutable lists of classes allowed as fields in Struct
 *     - immutable sets of classes allowed as fields in Struct
 *     - immutable maps with keys and value of classes allowed as fields in Struct
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Struct {
}