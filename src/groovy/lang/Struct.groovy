package groovy.lang

/**
 * Struct is marker interface for special type of classes with following properties
 * - super class is either Object or also implements Struct interface
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
abstract class Struct<T> {
}