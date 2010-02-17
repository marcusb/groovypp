package groovy.lang

/**
 * Annotation to help compiler generate default implementation for {@link Externalizable}
 *
 * By default, compiler generates missing implementation of readExternal, writeExternal for
 * every class implementing {@link Externalizable} (even abstract one). Annotation @Serial helps
 * to tune this generation for your needs instead of providing full implementation.
 *
 * Groovy++ rules for class to have default implementation of {@link Externalizable} are
 * - class has public no-arg constructor
 * - super class is {@link Externalizable} and have non abstract implementation of readExternal and writeExternal
 * - all non-transient fields/ properties are one of primitive types or {@link Serializable} or array of such type
 *
 * If one of rules above is not fulfiled a compile time warning will be issued
 */
public @interface Serial {
    enum Policy {
       /**
        * all non-transient fields will be serializied
        */
        FIELDS,

       /**
        * all properties will be serialized
        */
        PROPERTIES,

       /**
        * neither fields nor properties will be serialized
        */
        NONE
    }

   /**
    * Serialization policy
    */
    Policy value () default Policy.FIELDS

   /**
    * Names of fields/properties to exclude from serilization
    */
    String [] exclude () default null
}